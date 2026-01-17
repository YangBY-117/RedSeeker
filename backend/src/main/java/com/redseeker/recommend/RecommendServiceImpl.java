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
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
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

    // Always recommend all attractions in database; only use city for scoring/tags.
    List<AttractionRecord> attractions = loadAttractions(null);
    if (attractions.isEmpty()) {
      return Collections.emptyList();
    }

    Map<String, Double> averageRatings = loadAverageRatings();
    Map<String, Integer> ratingCounts = loadRatingCounts();
    RatingSummary ratingSummary = loadRatingSummary();
    Map<String, Integer> browseCounts = loadBrowseCounts();
    int maxBrowseCount =
        browseCounts.values().stream().max(Integer::compareTo).orElse(0);
    Map<String, AttractionStageInfo> stageInfoMap = loadAttractionStages();

    Map<Long, Map<String, Double>> allRatings = loadAllRatings();
    Map<String, Double> targetUserRatings = Collections.emptyMap();
    if (request.getUserId() != null) {
      targetUserRatings = allRatings.getOrDefault(request.getUserId(), Collections.emptyMap());
    }

    List<RecommendItem> results = new ArrayList<>();
    for (AttractionRecord attraction : attractions) {
      List<String> tags = loadAttractionTags(attraction.getId(), request.getCity());
      String category = formatCategory(attraction.getCategoryId());

      double baseScore = calculateBaseScore(
          attraction.getId(),
          averageRatings,
          ratingCounts,
          ratingSummary,
          browseCounts,
          maxBrowseCount);
      double contentScore = calculateContentScore(category, tags, request.getPreferences());
      double cfScore = 0.0;
      if (request.getUserId() != null && !allRatings.isEmpty()) {
        cfScore = calculateUserBasedCFScore(targetUserRatings, allRatings, attraction.getId());
      }
      double locationScore = calculateLocationScore(request, attraction);
      double timeScore = calculateTimeScore(request.getVisitTime(), attraction.getBusinessHours());

      double finalScore =
          calculateFinalScore(baseScore, contentScore, cfScore, locationScore, timeScore, request);

      String reason =
          generateReason(attraction, tags, request, finalScore, cfScore, locationScore, timeScore);
      
      // Get rating data
      Double avgRating = averageRatings.get(attraction.getId());
      Integer totalRatings = ratingCounts.get(attraction.getId());
      Integer browseCount = browseCounts.get(attraction.getId());
      AttractionStageInfo stageInfo = stageInfoMap.get(attraction.getId());
      
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
              attraction.getBusinessHours(),
              attraction.getPerCapitaConsumption(),
              avgRating,
              totalRatings,
              browseCount,
              attraction.getLongitude(),
              attraction.getLatitude(),
              stageInfo == null ? null : stageInfo.stageStart(),
              stageInfo == null ? null : stageInfo.stageEnd(),
              stageInfo == null ? null : stageInfo.stageName()));
    }

    // Sort by score from high to low (descending)
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
        "SELECT id, name, address, category, brief_intro, historical_background, business_hours, per_capita_consumption, longitude, latitude "
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
                  resultSet.getString("historical_background"),
                  resultSet.getString("business_hours"),
                  resultSet.getDouble("per_capita_consumption"),
                  resultSet.getObject("longitude", Double.class),
                  resultSet.getObject("latitude", Double.class)));
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

  private Map<String, Integer> loadRatingCounts() {
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
      LOGGER.warn("Failed to load rating counts", ex);
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
      Map<String, Integer> ratingCounts,
      RatingSummary ratingSummary,
      Map<String, Integer> browseCounts,
      int maxBrowseCount) {
    Double rating = averageRatings.get(attractionId);
    Integer count = ratingCounts.get(attractionId);
    double ratingScore = 0.0;
    if (rating != null && rating > 0) {
      double globalAvg = ratingSummary.averageRating() > 0 ? ratingSummary.averageRating() : 3.5;
      int baseCount = ratingSummary.priorCount() > 0 ? ratingSummary.priorCount() : 5;
      int safeCount = count == null ? 0 : count;
      double bayesian = (rating * safeCount + globalAvg * baseCount) / (safeCount + baseCount);
      ratingScore = Math.min(1.0, bayesian / 5.0);
    }

    double heatScore = 0.0;
    Integer browseCount = browseCounts.get(attractionId);
    if (browseCount != null && browseCount > 0 && maxBrowseCount > 0) {
      heatScore = Math.log1p(browseCount) / Math.log1p(maxBrowseCount);
    }

    if (ratingScore == 0.0 && heatScore == 0.0) {
      return 0.5;
    }
    return (ratingScore * 0.7) + (heatScore * 0.3);
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
      double cfScore,
      double locationScore,
      double timeScore) {
    if (locationScore > 0.7) {
      return "距离您当前位置较近，适合就近安排行程。";
    }
    if (timeScore > 0.7) {
      return "当前时间段适合参观，营业时间匹配。";
    }
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

  private double calculateFinalScore(
      double baseScore,
      double contentScore,
      double cfScore,
      double locationScore,
      double timeScore,
      RecommendRequest request) {
    double baseWeight = request.getUserId() == null ? 0.4 : 0.3;
    double contentWeight = request.getUserId() == null ? 0.4 : 0.3;
    double cfWeight = request.getUserId() == null ? 0.0 : 0.2;
    double locationWeight =
        (request.getUserLongitude() != null && request.getUserLatitude() != null) ? 0.15 : 0.0;
    double timeWeight =
        (request.getVisitTime() != null && !request.getVisitTime().isBlank()) ? 0.15 : 0.0;

    double totalWeight = baseWeight + contentWeight + cfWeight + locationWeight + timeWeight;
    if (totalWeight <= 0) {
      return 0.5;
    }
    return (baseScore * baseWeight
        + contentScore * contentWeight
        + cfScore * cfWeight
        + locationScore * locationWeight
        + timeScore * timeWeight) / totalWeight;
  }

  private double calculateLocationScore(RecommendRequest request, AttractionRecord attraction) {
    if (request.getUserLongitude() == null || request.getUserLatitude() == null) {
      return 0.5;
    }
    if (attraction.getLongitude() == null || attraction.getLatitude() == null) {
      return 0.4;
    }
    double distance = haversine(
        request.getUserLatitude(),
        request.getUserLongitude(),
        attraction.getLatitude(),
        attraction.getLongitude());
    double maxDistance = 50000.0;
    return Math.max(0.0, Math.min(1.0, 1.0 - (distance / maxDistance)));
  }

  private double calculateTimeScore(String visitTime, String businessHours) {
    if (visitTime == null || visitTime.isBlank()) {
      return 0.5;
    }
    if (businessHours == null || businessHours.isBlank()) {
      return 0.4;
    }
    if (businessHours.contains("全天")) {
      return 1.0;
    }
    LocalTime targetTime = parseVisitTime(visitTime);
    if (targetTime == null) {
      return 0.4;
    }
    TimeWindow window = parseBusinessHours(businessHours);
    if (window == null) {
      return 0.4;
    }
    return window.isWithin(targetTime) ? 1.0 : 0.2;
  }

  private LocalTime parseVisitTime(String visitTime) {
    try {
      return OffsetDateTime.parse(visitTime).atZoneSameInstant(ZoneId.systemDefault()).toLocalTime();
    } catch (DateTimeParseException ex) {
      try {
        return LocalTime.parse(visitTime);
      } catch (DateTimeParseException ignore) {
        return null;
      }
    }
  }

  private TimeWindow parseBusinessHours(String businessHours) {
    String normalized = businessHours.replaceAll("\\s", "");
    String[] parts = normalized.split("[-~至]");
    if (parts.length < 2) {
      return null;
    }
    LocalTime start = parseHourMinute(parts[0]);
    LocalTime end = parseHourMinute(parts[1]);
    if (start == null || end == null) {
      return null;
    }
    return new TimeWindow(start, end);
  }

  private LocalTime parseHourMinute(String value) {
    String cleaned = value.replaceAll("[^0-9:]", "");
    if (cleaned.isEmpty()) {
      return null;
    }
    if (!cleaned.contains(":") && cleaned.length() == 4) {
      cleaned = cleaned.substring(0, 2) + ":" + cleaned.substring(2);
    }
    try {
      return LocalTime.parse(cleaned);
    } catch (DateTimeParseException ex) {
      return null;
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

  private Map<String, AttractionStageInfo> loadAttractionStages() {
    String sql =
        "SELECT a.id AS attraction_id, MIN(he.start_year) AS start_year, "
            + "MAX(he.end_year) AS end_year, MIN(he.period) AS period "
            + "FROM attractions a "
            + "LEFT JOIN attraction_events ae ON ae.attraction_id = a.id "
            + "LEFT JOIN historical_events he ON he.id = ae.event_id "
            + "GROUP BY a.id";
    Map<String, AttractionStageInfo> result = new HashMap<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        String id = String.valueOf(resultSet.getInt("attraction_id"));
        Integer start = resultSet.getObject("start_year", Integer.class);
        Integer end = resultSet.getObject("end_year", Integer.class);
        String period = resultSet.getString("period");
        result.put(id, new AttractionStageInfo(start, end, period));
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load attraction stage info", ex);
    }
    return result;
  }

  private RatingSummary loadRatingSummary() {
    String sql = "SELECT AVG(rating) AS avg_rating, COUNT(*) AS cnt FROM attraction_ratings";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      if (resultSet.next()) {
        double avg = resultSet.getDouble("avg_rating");
        int cnt = resultSet.getInt("cnt");
        return new RatingSummary(avg, cnt, 5);
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load rating summary", ex);
    }
    return new RatingSummary(3.5, 0, 5);
  }

  private double haversine(double lat1, double lon1, double lat2, double lon2) {
    double earthRadius = 6371000.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadius * c;
  }

  private record RatingSummary(double averageRating, int totalCount, int priorCount) {}

  private record AttractionStageInfo(Integer stageStart, Integer stageEnd, String stageName) {}

  private record TimeWindow(LocalTime start, LocalTime end) {
    private boolean isWithin(LocalTime time) {
      if (start.equals(end)) {
        return true;
      }
      if (start.isBefore(end)) {
        return !time.isBefore(start) && !time.isAfter(end);
      }
      return !time.isBefore(start) || !time.isAfter(end);
    }
  }

  private static final class AttractionRecord {
    private final String id;
    private final String name;
    private final String address;
    private final int categoryId;
    private final String briefIntro;
    private final String history;
    private final String businessHours;
    private final Double perCapitaConsumption;
    private final Double longitude;
    private final Double latitude;

    private AttractionRecord(
        String id,
        String name,
        String address,
        int categoryId,
        String briefIntro,
        String history,
        String businessHours,
        Double perCapitaConsumption,
        Double longitude,
        Double latitude) {
      this.id = id;
      this.name = name;
      this.address = address;
      this.categoryId = categoryId;
      this.briefIntro = briefIntro;
      this.history = history;
      this.businessHours = businessHours;
      this.perCapitaConsumption = perCapitaConsumption;
      this.longitude = longitude;
      this.latitude = latitude;
    }

    private String getId() {
      return id;
    }

    private String getName() {
      return name;
    }

    private String getAddress() {
      return address;
    }

    private int getCategoryId() {
      return categoryId;
    }

    private String getBusinessHours() {
      return businessHours;
    }

    private Double getPerCapitaConsumption() {
      return perCapitaConsumption;
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

    private Double getLongitude() {
      return longitude;
    }

    private Double getLatitude() {
      return latitude;
    }
  }
}
