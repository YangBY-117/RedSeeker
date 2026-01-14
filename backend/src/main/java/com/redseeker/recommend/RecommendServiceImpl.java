package com.redseeker.recommend;

import com.redseeker.common.ErrorCode;
import com.redseeker.common.ServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecommendServiceImpl implements RecommendService {
  private static final Logger LOGGER = LoggerFactory.getLogger(RecommendServiceImpl.class);

  private final AiService aiService;
  private final String databaseUrl;

  public RecommendServiceImpl(AiService aiService) {
    this.aiService = aiService;
    this.databaseUrl = resolveDatabaseUrl();
  }

  @Override
  public List<RecommendItem> getRecommendations(RecommendRequest request) {
    if (request.getCity() == null || request.getCity().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "city is required");
    }

    List<AttractionRecord> attractions = loadAttractions(request.getCity());
    if (attractions.isEmpty()) {
      attractions = loadAttractions(null);
    }
    if (attractions.isEmpty()) {
      return Collections.emptyList();
    }

    Map<String, Double> averageRatings = loadAverageRatings();
    Map<String, Integer> browseCounts = loadBrowseCounts();
    Map<String, Integer> totalRatingsCount = loadTotalRatingsCount();
    int maxBrowseCount =
        browseCounts.values().stream().max(Integer::compareTo).orElse(0);

    Map<Long, Map<String, Double>> allRatings = loadAllRatings();
    Map<String, Double> targetUserRatings = Collections.emptyMap();
    if (request.getUserId() != null) {
      targetUserRatings = allRatings.getOrDefault(request.getUserId(), Collections.emptyMap());
    }

    List<RecommendItem> results = new ArrayList<>();
    for (AttractionRecord attraction : attractions) {
      List<String> tags = loadAttractionTags(attraction.getId(), request.getCity());
      String category = formatCategory(attraction.getCategoryId());

      double baseScore = calculateBaseScore(attraction.getId(), averageRatings, browseCounts, maxBrowseCount);
      double contentScore = calculateContentScore(category, tags, request.getPreferences());
      double cfScore = 0.0;
      if (request.getUserId() != null && !allRatings.isEmpty()) {
        cfScore = calculateUserBasedCFScore(targetUserRatings, allRatings, attraction.getId());
      }

      double finalScore;
      if (request.getUserId() == null) {
        finalScore = (baseScore * 0.4) + (contentScore * 0.6);
      } else {
        finalScore = (baseScore * 0.2) + (contentScore * 0.4) + (cfScore * 0.4);
      }

      String reason = generateReason(attraction, tags, request, finalScore, cfScore);
      
      // Get rating and heat data from database
      Double avgRating = averageRatings.get(attraction.getId());
      Integer totalRatings = totalRatingsCount.getOrDefault(attraction.getId(), 0);
      Integer heatScore = browseCounts.getOrDefault(attraction.getId(), 0);
      
      results.add(
          new RecommendItem(
              attraction.getId(),
              attraction.getName(),
              category,
              tags,
              finalScore,
              attraction.getHistory(),
              reason,
              attraction.getAddress(),
              avgRating,
              totalRatings,
              heatScore));
    }

    return results.stream()
        .sorted(Comparator.comparingDouble(RecommendItem::getScore).reversed())
        .collect(Collectors.toList());
  }

  @Override
  public AiPlanResponse generateAiPlan(AiPlanRequest request) {
    String prompt =
        String.format(
            "Please design a red tourism itinerary. City: %s. Days: %d. Needs: %s.",
            request.getCity() != null ? request.getCity() : "Shanghai",
            request.getDays() != null ? request.getDays() : 2,
            request.getPrompt());

    String aiOutput = aiService.generateContent(prompt);

    AiPlanResponse response = new AiPlanResponse();
    response.setSummary("AI generated a personalized plan.");

    List<ItineraryPlan> plans = new ArrayList<>();
    ItineraryPlan plan = new ItineraryPlan();
    plan.setDay(1);
    plan.setTitle("AI itinerary");
    plan.setDescription(aiOutput);
    plans.add(plan);

    response.setPlans(plans);
    return response;
  }

  private List<AttractionRecord> loadAttractions(String city) {
    String baseSql =
        "SELECT id, name, address, category, brief_intro, historical_background "
            + "FROM attractions";
    boolean filterCity = city != null && !city.isBlank();
    String sql = baseSql;
    if (filterCity) {
      sql += " WHERE address LIKE ? OR name LIKE ?";
    }

    List<AttractionRecord> records = new ArrayList<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      if (filterCity) {
        String pattern = "%" + city.trim() + "%";
        statement.setString(1, pattern);
        statement.setString(2, pattern);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          records.add(
              new AttractionRecord(
                  String.valueOf(resultSet.getInt("id")),
                  resultSet.getString("name"),
                  resultSet.getString("address"),
                  resultSet.getInt("category"),
                  resultSet.getString("brief_intro"),
                  resultSet.getString("historical_background")));
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to load attractions", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to load attractions");
    }
    return records;
  }

  private List<String> loadAttractionTags(String attractionId, String city) {
    Set<String> tags = new HashSet<>();
    if (city != null && !city.isBlank()) {
      tags.add(city.trim());
    }
    String sql =
        "SELECT he.event_name, he.period "
            + "FROM historical_events he "
            + "JOIN attraction_events ae ON ae.event_id = he.id "
            + "WHERE ae.attraction_id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, Integer.parseInt(attractionId));
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          addIfPresent(tags, resultSet.getString("event_name"));
          addIfPresent(tags, resultSet.getString("period"));
        }
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load tags for attraction {}", attractionId, ex);
    }
    return tags.stream().sorted().collect(Collectors.toList());
  }

  private Map<String, Double> loadAverageRatings() {
    String sql = "SELECT attraction_id, AVG(rating) AS avg_rating FROM attraction_ratings GROUP BY attraction_id";
    Map<String, Double> result = new HashMap<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        result.put(
            String.valueOf(resultSet.getInt("attraction_id")),
            resultSet.getDouble("avg_rating"));
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load average ratings", ex);
    }
    return result;
  }

  private Map<String, Integer> loadBrowseCounts() {
    String sql = "SELECT attraction_id, COUNT(*) AS cnt FROM user_browse_history GROUP BY attraction_id";
    Map<String, Integer> result = new HashMap<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        result.put(
            String.valueOf(resultSet.getInt("attraction_id")),
            resultSet.getInt("cnt"));
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load browse counts", ex);
    }
    return result;
  }

  private Map<String, Integer> loadTotalRatingsCount() {
    String sql = "SELECT attraction_id, COUNT(*) AS cnt FROM attraction_ratings GROUP BY attraction_id";
    Map<String, Integer> result = new HashMap<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        result.put(
            String.valueOf(resultSet.getInt("attraction_id")),
            resultSet.getInt("cnt"));
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load total ratings count", ex);
    }
    return result;
  }

  private Map<Long, Map<String, Double>> loadAllRatings() {
    String sql = "SELECT user_id, attraction_id, rating FROM attraction_ratings";
    Map<Long, Map<String, Double>> result = new HashMap<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        long userId = resultSet.getLong("user_id");
        String attractionId = String.valueOf(resultSet.getInt("attraction_id"));
        double rating = resultSet.getDouble("rating");
        result.computeIfAbsent(userId, key -> new HashMap<>()).put(attractionId, rating);
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load ratings", ex);
    }
    return result;
  }

  private double calculateBaseScore(
      String attractionId,
      Map<String, Double> averageRatings,
      Map<String, Integer> browseCounts,
      int maxBrowseCount) {
    Double rating = averageRatings.get(attractionId);
    if (rating != null && rating > 0) {
      return Math.min(1.0, rating / 5.0);
    }
    Integer browseCount = browseCounts.get(attractionId);
    if (browseCount != null && browseCount > 0 && maxBrowseCount > 0) {
      double normalized = (double) browseCount / (double) maxBrowseCount;
      return Math.min(1.0, 0.2 + (normalized * 0.8));
    }
    return 0.5;
  }

  private double calculateContentScore(String category, List<String> tags, List<String> preferences) {
    if (preferences == null || preferences.isEmpty()) {
      return 0.5;
    }

    int matches = 0;
    for (String preference : preferences) {
      if (preference == null || preference.isBlank()) {
        continue;
      }
      if (matchesPreference(category, preference)) {
        matches += 2;
      }
      for (String tag : tags) {
        if (matchesPreference(tag, preference)) {
          matches += 1;
        }
      }
    }

    return Math.min(1.0, matches / 5.0);
  }

  private double calculateUserBasedCFScore(
      Map<String, Double> targetUserRatings,
      Map<Long, Map<String, Double>> allRatings,
      String targetAttractionId) {
    if (targetUserRatings == null || targetUserRatings.isEmpty()) {
      return 0.5;
    }

    if (targetUserRatings.containsKey(targetAttractionId)) {
      return targetUserRatings.get(targetAttractionId) / 5.0;
    }

    double weightedSum = 0.0;
    double similaritySum = 0.0;

    for (Map.Entry<Long, Map<String, Double>> entry : allRatings.entrySet()) {
      Map<String, Double> otherRatings = entry.getValue();
      if (otherRatings == targetUserRatings) {
        continue;
      }
      double similarity = calculateCosineSimilarity(targetUserRatings, otherRatings);
      if (similarity > 0 && otherRatings.containsKey(targetAttractionId)) {
        weightedSum += similarity * otherRatings.get(targetAttractionId);
        similaritySum += similarity;
      }
    }

    if (similaritySum == 0.0) {
      return 0.5;
    }

    double predictedRating = weightedSum / similaritySum;
    return predictedRating / 5.0;
  }

  private double calculateCosineSimilarity(
      Map<String, Double> ratings1, Map<String, Double> ratings2) {
    double dotProduct = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;

    for (Map.Entry<String, Double> entry : ratings1.entrySet()) {
      String itemId = entry.getKey();
      double rating = entry.getValue();
      norm1 += rating * rating;
      Double otherRating = ratings2.get(itemId);
      if (otherRating != null) {
        dotProduct += rating * otherRating;
      }
    }

    for (double rating : ratings2.values()) {
      norm2 += rating * rating;
    }

    if (norm1 == 0.0 || norm2 == 0.0) {
      return 0.0;
    }
    return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
  }

  private String generateReason(
      AttractionRecord attraction,
      List<String> tags,
      RecommendRequest request,
      double finalScore,
      double cfScore) {
    if (request.getPreferences() != null && !request.getPreferences().isEmpty()) {
      List<String> hits =
          request.getPreferences().stream()
              .filter(pref -> matchesAny(pref, tags))
              .collect(Collectors.toList());
      if (!hits.isEmpty()) {
        return "Matches your interests: " + String.join(", ", hits);
      }
    }
    if (request.getUserId() != null && cfScore > 0.7) {
      return "Recommended based on similar users' preferences.";
    }
    if (finalScore > 0.8) {
      return "High overall relevance for your trip.";
    }
    if (attraction.getHistory() != null && !attraction.getHistory().isBlank()) {
      return attraction.getHistory();
    }
    return "Recommended for your itinerary.";
  }

  private boolean matchesAny(String preference, List<String> tags) {
    if (preference == null || preference.isBlank()) {
      return false;
    }
    for (String tag : tags) {
      if (matchesPreference(tag, preference)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesPreference(String text, String preference) {
    if (text == null || preference == null) {
      return false;
    }
    String trimmedText = text.trim();
    String trimmedPref = preference.trim();
    if (trimmedText.isEmpty() || trimmedPref.isEmpty()) {
      return false;
    }
    return trimmedText.contains(trimmedPref) || trimmedPref.contains(trimmedText);
  }

  private void addIfPresent(Set<String> tags, String value) {
    if (value != null && !value.isBlank()) {
      tags.add(value.trim());
    }
  }

  private String formatCategory(int categoryId) {
    switch (categoryId) {
      case 1:
        return "Revolutionary Site";
      case 2:
        return "Celebrity Residence";
      case 3:
        return "Memorial Hall";
      case 4:
        return "Martyr Cemetery";
      case 5:
        return "Patriotic Education Base";
      default:
        return "Category-" + categoryId;
    }
  }

  private Connection openConnection() throws SQLException {
    return DriverManager.getConnection(databaseUrl);
  }

  private String resolveDatabaseUrl() {
    String override = System.getenv("REDSEEKER_DB_PATH");
    if (override != null && !override.isBlank()) {
      return "jdbc:sqlite:" + override;
    }
    Path direct = Paths.get("database", "red_tourism.db");
    if (Files.exists(direct)) {
      return "jdbc:sqlite:" + direct.toAbsolutePath();
    }
    Path parent = Paths.get("..", "database", "red_tourism.db");
    if (Files.exists(parent)) {
      return "jdbc:sqlite:" + parent.toAbsolutePath();
    }
    return "jdbc:sqlite:database/red_tourism.db";
  }

  private static final class AttractionRecord {
    private final String id;
    private final String name;
    private final String address;
    private final int categoryId;
    private final String briefIntro;
    private final String history;

    private AttractionRecord(
        String id,
        String name,
        String address,
        int categoryId,
        String briefIntro,
        String history) {
      this.id = id;
      this.name = name;
      this.address = address;
      this.categoryId = categoryId;
      this.briefIntro = briefIntro;
      this.history = history;
    }

    private String getId() {
      return id;
    }

    private String getName() {
      return name;
    }

    private int getCategoryId() {
      return categoryId;
    }

    private String getAddress() {
      return address;
    }

    private String getHistory() {
      if (history != null && !history.isBlank()) {
        return history;
      }
      if (briefIntro != null && !briefIntro.isBlank()) {
        return briefIntro;
      }
      return address;
    }
  }
}
