package com.redseeker.diary;

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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DiaryServiceImpl implements DiaryService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiaryServiceImpl.class);

  private final String databaseUrl;

  public DiaryServiceImpl() {
    this.databaseUrl = resolveDatabaseUrl();
  }

  @Override
  public DiaryListResponse listDiaries(String sortBy, String destination, Long userId,
      int page, int pageSize) {
    String normalizedDestination = normalize(destination);
    int safePage = page <= 0 ? 1 : page;
    int safePageSize = pageSize <= 0 ? 10 : pageSize;

    List<DiarySummary> summaries = loadDiarySummaries(normalizedDestination);
    Map<Long, Set<Long>> diaryAttractions = loadDiaryAttractions(summaries);
    Set<Long> userBrowseAttractions = loadUserBrowseAttractions(userId);
    double maxViews = summaries.stream().mapToDouble(DiarySummary::getViewCount).max().orElse(0.0);

    for (DiarySummary summary : summaries) {
      double recommendScore = calculateRecommendScore(
          summary,
          diaryAttractions.getOrDefault(summary.getId(), Collections.emptySet()),
          userBrowseAttractions,
          maxViews);
      summary.setRecommendScore(recommendScore);
    }

    sortSummaries(summaries, sortBy);

    return paginateSummaries(summaries, safePage, safePageSize);
  }

  @Override
  public DiaryListResponse searchByDestination(String destination, String sortBy, int page,
      int pageSize) {
    if (destination == null || destination.isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "destination is required");
    }
    return listDiaries(sortBy, destination, null, page, pageSize);
  }

  @Override
  public DiaryDetail searchByName(String title) {
    if (title == null || title.isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "title is required");
    }
    DiaryDetail detail = loadDiaryDetailByTitle(title.trim());
    if (detail == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "diary not found");
    }
    return detail;
  }

  @Override
  public DiaryListResponse fulltextSearch(String keyword, int page, int pageSize) {
    if (keyword == null || keyword.isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "keyword is required");
    }
    int safePage = page <= 0 ? 1 : page;
    int safePageSize = pageSize <= 0 ? 10 : pageSize;
    List<DiarySummary> summaries = loadFulltextSummaries(keyword.trim());
    return paginateSummaries(summaries, safePage, safePageSize);
  }

  @Override
  public DiaryDetail getDiary(Long id, Long userId) {
    DiaryDetail detail = loadDiaryDetail(id);
    if (detail == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "diary not found");
    }
    incrementViewCount(id, userId);
    if (userId != null) {
      detail.setUserRating(loadUserRating(id, userId));
    }
    return detail;
  }

  @Override
  public DiaryCreateResponse createDiary(DiaryCreateRequest request) {
    validateCreateRequest(request);
    String sql = "INSERT INTO travel_diaries (user_id, title, content, destination, travel_date, "
        + "view_count, average_rating, total_ratings, created_at, updated_at) "
        + "VALUES (?, ?, ?, ?, ?, 0, 0, 0, datetime('now'), datetime('now'))";
    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setLong(1, request.getUserId());
      statement.setString(2, request.getTitle());
      statement.setString(3, request.getContent());
      statement.setString(4, request.getDestination());
      statement.setString(5, request.getTravelDate());
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          long diaryId = keys.getLong(1);
          insertDiaryRelations(connection, diaryId, request.getAttractionIds(), request.getMediaInputs());
          return loadCreateResponse(connection, diaryId);
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to create diary", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to create diary");
    }
    throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to create diary");
  }

  @Override
  public DiaryCreateResponse updateDiary(Long id, DiaryCreateRequest request) {
    validateCreateRequest(request);
    DiaryOwnership ownership = loadDiaryOwnership(id);
    if (ownership == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "diary not found");
    }
    if (!ownership.userId.equals(request.getUserId())) {
      throw new ServiceException(ErrorCode.FORBIDDEN, "no permission to update diary");
    }
    String sql =
        "UPDATE travel_diaries SET title = ?, content = ?, destination = ?, travel_date = ?, "
            + "updated_at = datetime('now') WHERE id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, request.getTitle());
      statement.setString(2, request.getContent());
      statement.setString(3, request.getDestination());
      statement.setString(4, request.getTravelDate());
      statement.setLong(5, id);
      statement.executeUpdate();

      deleteDiaryRelations(connection, id);
      insertDiaryRelations(connection, id, request.getAttractionIds(), request.getMediaInputs());
      return loadCreateResponse(connection, id);
    } catch (SQLException ex) {
      LOGGER.error("Failed to update diary", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to update diary");
    }
  }

  @Override
  public void deleteDiary(Long id, Long userId) {
    DiaryOwnership ownership = loadDiaryOwnership(id);
    if (ownership == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "diary not found");
    }
    if (userId == null || !ownership.userId.equals(userId)) {
      throw new ServiceException(ErrorCode.FORBIDDEN, "no permission to delete diary");
    }
    try (Connection connection = openConnection()) {
      deleteDiaryRelations(connection, id);
      try (PreparedStatement statement =
          connection.prepareStatement("DELETE FROM travel_diaries WHERE id = ?")) {
        statement.setLong(1, id);
        statement.executeUpdate();
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to delete diary", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to delete diary");
    }
  }

  @Override
  public DiaryRateResponse rateDiary(Long diaryId, DiaryRatingRequest request) {
    if (request == null || request.getUserId() == null || request.getRating() == null) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "rating request is required");
    }
    ensureDiaryExists(diaryId);
    try (Connection connection = openConnection()) {
      Long ratingId = findRatingId(connection, diaryId, request.getUserId());
      if (ratingId == null) {
        insertRating(connection, diaryId, request.getUserId(), request.getRating());
      } else {
        updateRating(connection, ratingId, request.getRating());
      }
      DiaryRateResponse response = refreshDiaryRating(connection, diaryId);
      updateDiaryRating(connection, diaryId, response);
      return response;
    } catch (SQLException ex) {
      LOGGER.error("Failed to rate diary", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to rate diary");
    }
  }

  private void sortSummaries(List<DiarySummary> summaries, String sortBy) {
    String key = sortBy == null ? "heat" : sortBy.trim().toLowerCase();
    ToDoubleFunction<DiarySummary> scoring;
    switch (key) {
      case "rating":
        scoring = DiarySummary::getAverageRating;
        break;
      case "time":
        scoring = summary -> parseTimeScore(summary.getCreatedAt());
        break;
      case "heat":
      default:
        scoring = DiarySummary::getRecommendScore;
        break;
    }
    quickSort(summaries, 0, summaries.size() - 1, scoring);
  }

  private double parseTimeScore(String createdAt) {
    if (createdAt == null) {
      return 0.0;
    }
    return createdAt.hashCode();
  }

  private void quickSort(List<DiarySummary> items, int low, int high,
      ToDoubleFunction<DiarySummary> scoring) {
    if (low >= high) {
      return;
    }
    int pivotIndex = partition(items, low, high, scoring);
    quickSort(items, low, pivotIndex - 1, scoring);
    quickSort(items, pivotIndex + 1, high, scoring);
  }

  private int partition(List<DiarySummary> items, int low, int high,
      ToDoubleFunction<DiarySummary> scoring) {
    double pivot = scoring.applyAsDouble(items.get(high));
    int i = low - 1;
    for (int j = low; j < high; j++) {
      if (scoring.applyAsDouble(items.get(j)) >= pivot) {
        i++;
        Collections.swap(items, i, j);
      }
    }
    Collections.swap(items, i + 1, high);
    return i + 1;
  }

  private DiaryListResponse paginateSummaries(List<DiarySummary> summaries, int page,
      int pageSize) {
    int total = summaries.size();
    int totalPages = (int) Math.ceil((double) total / (double) pageSize);
    int fromIndex = Math.min((page - 1) * pageSize, total);
    int toIndex = Math.min(fromIndex + pageSize, total);
    List<DiarySummary> pageItems = summaries.subList(fromIndex, toIndex);
    return new DiaryListResponse(pageItems, total, page, pageSize, totalPages);
  }

  private List<DiarySummary> loadDiarySummaries(String destination) {
    StringBuilder sql = new StringBuilder(
        "SELECT d.id, d.title, d.content, d.destination, d.travel_date, d.view_count, "
            + "d.average_rating, d.total_ratings, d.created_at, u.id AS user_id, u.username "
            + "FROM travel_diaries d JOIN users u ON d.user_id = u.id");
    List<Object> params = new ArrayList<>();
    if (destination != null) {
      sql.append(" WHERE d.destination LIKE ?");
      params.add("%" + destination + "%");
    }
    List<DiarySummary> results = new ArrayList<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql.toString())) {
      for (int i = 0; i < params.size(); i++) {
        statement.setObject(i + 1, params.get(i));
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        Map<Long, String> coverImages = loadCoverImages(connection);
        while (resultSet.next()) {
          long id = resultSet.getLong("id");
          results.add(new DiarySummary(
              id,
              resultSet.getString("title"),
              previewContent(resultSet.getString("content")),
              resultSet.getString("destination"),
              resultSet.getString("travel_date"),
              resultSet.getInt("view_count"),
              resultSet.getDouble("average_rating"),
              resultSet.getInt("total_ratings"),
              new DiaryAuthor(resultSet.getLong("user_id"), resultSet.getString("username")),
              coverImages.get(id),
              resultSet.getString("created_at"),
              0.0));
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to load diaries", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to load diaries");
    }
    return results;
  }

  private Map<Long, String> loadCoverImages(Connection connection) throws SQLException {
    String sql = "SELECT diary_id, file_path FROM diary_media WHERE media_type = 'image' "
        + "ORDER BY diary_id, display_order";
    Map<Long, String> coverImages = new HashMap<>();
    try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        long diaryId = resultSet.getLong("diary_id");
        coverImages.putIfAbsent(diaryId, resultSet.getString("file_path"));
      }
    }
    return coverImages;
  }

  private List<DiarySummary> loadFulltextSummaries(String keyword) {
    String sql =
        "SELECT d.id, d.title, d.content, d.destination, d.travel_date, d.view_count, "
            + "d.average_rating, d.total_ratings, d.created_at, u.id AS user_id, u.username, "
            + "bm25(fts) AS score "
            + "FROM travel_diaries_fts fts "
            + "JOIN travel_diaries d ON d.id = fts.rowid "
            + "JOIN users u ON d.user_id = u.id "
            + "WHERE fts MATCH ? ORDER BY score";
    List<DiarySummary> results = new ArrayList<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, keyword);
      try (ResultSet resultSet = statement.executeQuery()) {
        Map<Long, String> coverImages = loadCoverImages(connection);
        while (resultSet.next()) {
          long id = resultSet.getLong("id");
          results.add(new DiarySummary(
              id,
              resultSet.getString("title"),
              previewContent(resultSet.getString("content")),
              resultSet.getString("destination"),
              resultSet.getString("travel_date"),
              resultSet.getInt("view_count"),
              resultSet.getDouble("average_rating"),
              resultSet.getInt("total_ratings"),
              new DiaryAuthor(resultSet.getLong("user_id"), resultSet.getString("username")),
              coverImages.get(id),
              resultSet.getString("created_at"),
              0.0));
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to fulltext search diaries", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to search diaries");
    }
    return results;
  }

  private DiaryDetail loadDiaryDetail(Long id) {
    String sql =
        "SELECT d.id, d.title, d.content, d.destination, d.travel_date, d.view_count, "
            + "d.average_rating, d.total_ratings, d.created_at, d.updated_at, u.id AS user_id, "
            + "u.username "
            + "FROM travel_diaries d JOIN users u ON d.user_id = u.id WHERE d.id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return null;
        }
        DiaryDetail detail = new DiaryDetail();
        detail.setId(resultSet.getLong("id"));
        detail.setTitle(resultSet.getString("title"));
        detail.setContent(resultSet.getString("content"));
        detail.setDestination(resultSet.getString("destination"));
        detail.setTravelDate(resultSet.getString("travel_date"));
        detail.setViewCount(resultSet.getInt("view_count"));
        detail.setAverageRating(resultSet.getDouble("average_rating"));
        detail.setTotalRatings(resultSet.getInt("total_ratings"));
        detail.setCreatedAt(resultSet.getString("created_at"));
        detail.setUpdatedAt(resultSet.getString("updated_at"));
        detail.setAuthor(
            new DiaryAuthor(resultSet.getLong("user_id"), resultSet.getString("username")));
        detail.setMedia(loadDiaryMedia(connection, id));
        detail.setAttractions(loadDiaryAttractions(connection, id));
        return detail;
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to load diary", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to load diary");
    }
  }

  private DiaryDetail loadDiaryDetailByTitle(String title) {
    String sql =
        "SELECT d.id, d.title, d.content, d.destination, d.travel_date, d.view_count, "
            + "d.average_rating, d.total_ratings, d.created_at, d.updated_at, u.id AS user_id, "
            + "u.username "
            + "FROM travel_diaries d JOIN users u ON d.user_id = u.id WHERE d.title = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, title);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return null;
        }
        DiaryDetail detail = new DiaryDetail();
        detail.setId(resultSet.getLong("id"));
        detail.setTitle(resultSet.getString("title"));
        detail.setContent(resultSet.getString("content"));
        detail.setDestination(resultSet.getString("destination"));
        detail.setTravelDate(resultSet.getString("travel_date"));
        detail.setViewCount(resultSet.getInt("view_count"));
        detail.setAverageRating(resultSet.getDouble("average_rating"));
        detail.setTotalRatings(resultSet.getInt("total_ratings"));
        detail.setCreatedAt(resultSet.getString("created_at"));
        detail.setUpdatedAt(resultSet.getString("updated_at"));
        detail.setAuthor(
            new DiaryAuthor(resultSet.getLong("user_id"), resultSet.getString("username")));
        detail.setMedia(loadDiaryMedia(connection, detail.getId()));
        detail.setAttractions(loadDiaryAttractions(connection, detail.getId()));
        return detail;
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to load diary by title", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to load diary");
    }
  }

  private List<DiaryMedia> loadDiaryMedia(Connection connection, Long diaryId) throws SQLException {
    String sql =
        "SELECT id, media_type, file_path, thumbnail_path, display_order "
            + "FROM diary_media WHERE diary_id = ? ORDER BY display_order";
    List<DiaryMedia> media = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          media.add(new DiaryMedia(
              resultSet.getLong("id"),
              resultSet.getString("media_type"),
              resultSet.getString("file_path"),
              resultSet.getString("thumbnail_path"),
              resultSet.getInt("display_order")));
        }
      }
    }
    return media;
  }

  private List<DiaryAttraction> loadDiaryAttractions(Connection connection, Long diaryId)
      throws SQLException {
    String sql =
        "SELECT a.id, a.name, a.address "
            + "FROM diary_attractions da JOIN attractions a ON a.id = da.attraction_id "
            + "WHERE da.diary_id = ?";
    List<DiaryAttraction> attractions = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          attractions.add(new DiaryAttraction(
              resultSet.getLong("id"),
              resultSet.getString("name"),
              resultSet.getString("address")));
        }
      }
    }
    return attractions;
  }

  private void incrementViewCount(Long diaryId, Long userId) {
    try (Connection connection = openConnection()) {
      try (PreparedStatement statement =
          connection.prepareStatement("UPDATE travel_diaries SET view_count = view_count + 1 WHERE id = ?")) {
        statement.setLong(1, diaryId);
        statement.executeUpdate();
      }
      try (PreparedStatement statement =
          connection.prepareStatement(
              "INSERT INTO diary_browse_history (diary_id, user_id, browse_time) "
                  + "VALUES (?, ?, datetime('now'))")) {
        statement.setLong(1, diaryId);
        if (userId == null) {
          statement.setNull(2, java.sql.Types.INTEGER);
        } else {
          statement.setLong(2, userId);
        }
        statement.executeUpdate();
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to update diary view count", ex);
    }
  }

  private Integer loadUserRating(Long diaryId, Long userId) {
    String sql = "SELECT rating FROM diary_ratings WHERE diary_id = ? AND user_id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      statement.setLong(2, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt("rating");
        }
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load user rating", ex);
    }
    return null;
  }

  private double calculateRecommendScore(DiarySummary summary, Set<Long> diaryAttractions,
      Set<Long> userBrowseAttractions, double maxViews) {
    double heatScore = maxViews > 0 ? (summary.getViewCount() / maxViews) * 100.0 : 0.0;
    double ratingScore = (summary.getAverageRating() / 5.0) * 100.0;
    double interestScore = 0.0;
    if (!userBrowseAttractions.isEmpty() && !diaryAttractions.isEmpty()) {
      Set<Long> intersection = new HashSet<>(diaryAttractions);
      intersection.retainAll(userBrowseAttractions);
      if (!intersection.isEmpty()) {
        interestScore = 100.0;
      }
    }
    return (heatScore * 0.4) + (ratingScore * 0.4) + (interestScore * 0.2);
  }

  private Map<Long, Set<Long>> loadDiaryAttractions(List<DiarySummary> summaries) {
    if (summaries.isEmpty()) {
      return Collections.emptyMap();
    }
    String ids = summaries.stream().map(DiarySummary::getId).map(String::valueOf)
        .collect(Collectors.joining(","));
    String sql =
        "SELECT diary_id, attraction_id FROM diary_attractions WHERE diary_id IN (" + ids + ")";
    Map<Long, Set<Long>> result = new HashMap<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        long diaryId = resultSet.getLong("diary_id");
        long attractionId = resultSet.getLong("attraction_id");
        result.computeIfAbsent(diaryId, key -> new HashSet<>()).add(attractionId);
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load diary attractions", ex);
    }
    return result;
  }

  private Set<Long> loadUserBrowseAttractions(Long userId) {
    if (userId == null) {
      return Collections.emptySet();
    }
    String sql = "SELECT attraction_id FROM user_browse_history WHERE user_id = ?";
    Set<Long> results = new HashSet<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          results.add(resultSet.getLong("attraction_id"));
        }
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load user browse history", ex);
    }
    return results;
  }

  private String previewContent(String content) {
    if (content == null) {
      return "";
    }
    int limit = Math.min(content.length(), 120);
    return content.substring(0, limit);
  }

  private void validateCreateRequest(DiaryCreateRequest request) {
    if (request == null) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "request is required");
    }
    if (request.getUserId() == null) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "userId is required");
    }
    if (request.getTitle() == null || request.getTitle().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "title is required");
    }
    if (request.getContent() == null || request.getContent().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "content is required");
    }
    if (request.getDestination() == null || request.getDestination().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "destination is required");
    }
    if (request.getTravelDate() == null || request.getTravelDate().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "travelDate is required");
    }
  }

  private void insertDiaryRelations(Connection connection, long diaryId, List<Long> attractionIds,
      List<DiaryMediaInput> mediaInputs) throws SQLException {
    if (attractionIds != null && !attractionIds.isEmpty()) {
      try (PreparedStatement statement =
          connection.prepareStatement("INSERT INTO diary_attractions (diary_id, attraction_id) VALUES (?, ?)");) {
        for (Long attractionId : attractionIds) {
          if (attractionId == null) {
            continue;
          }
          statement.setLong(1, diaryId);
          statement.setLong(2, attractionId);
          statement.addBatch();
        }
        statement.executeBatch();
      }
    }
    if (mediaInputs != null && !mediaInputs.isEmpty()) {
      String sql =
          "INSERT INTO diary_media (diary_id, media_type, file_path, file_size, thumbnail_path, display_order, created_at) "
              + "VALUES (?, ?, ?, ?, ?, ?, datetime('now'))";
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        for (DiaryMediaInput media : mediaInputs) {
          if (media.getFilePath() == null || media.getFilePath().isBlank()) {
            continue;
          }
          statement.setLong(1, diaryId);
          statement.setString(2, media.getMediaType());
          statement.setString(3, media.getFilePath());
          statement.setLong(4, media.getFileSize());
          statement.setString(5, media.getThumbnailPath());
          statement.setInt(6, media.getDisplayOrder());
          statement.addBatch();
        }
        statement.executeBatch();
      }
    }
  }

  private void deleteDiaryRelations(Connection connection, long diaryId) throws SQLException {
    String[] tables = {"diary_media", "diary_attractions", "diary_ratings", "diary_browse_history"};
    for (String table : tables) {
      try (PreparedStatement statement =
          connection.prepareStatement("DELETE FROM " + table + " WHERE diary_id = ?")) {
        statement.setLong(1, diaryId);
        statement.executeUpdate();
      }
    }
  }

  private DiaryCreateResponse loadCreateResponse(Connection connection, long diaryId)
      throws SQLException {
    String sql = "SELECT title, created_at FROM travel_diaries WHERE id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return new DiaryCreateResponse(diaryId, resultSet.getString("title"),
              resultSet.getString("created_at"));
        }
      }
    }
    return new DiaryCreateResponse(diaryId, null, null);
  }

  private DiaryOwnership loadDiaryOwnership(Long diaryId) {
    String sql = "SELECT id, user_id FROM travel_diaries WHERE id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return new DiaryOwnership(resultSet.getLong("id"), resultSet.getLong("user_id"));
        }
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load diary ownership", ex);
    }
    return null;
  }

  private void ensureDiaryExists(Long diaryId) {
    if (loadDiaryOwnership(diaryId) == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "diary not found");
    }
  }

  private Long findRatingId(Connection connection, long diaryId, long userId) throws SQLException {
    String sql = "SELECT id FROM diary_ratings WHERE diary_id = ? AND user_id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      statement.setLong(2, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getLong("id");
        }
      }
    }
    return null;
  }

  private void insertRating(Connection connection, long diaryId, long userId, int rating)
      throws SQLException {
    String sql =
        "INSERT INTO diary_ratings (diary_id, user_id, rating, created_at) VALUES (?, ?, ?, datetime('now'))";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      statement.setLong(2, userId);
      statement.setInt(3, rating);
      statement.executeUpdate();
    }
  }

  private void updateRating(Connection connection, long ratingId, int rating) throws SQLException {
    String sql = "UPDATE diary_ratings SET rating = ?, created_at = datetime('now') WHERE id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, rating);
      statement.setLong(2, ratingId);
      statement.executeUpdate();
    }
  }

  private DiaryRateResponse refreshDiaryRating(Connection connection, long diaryId) throws SQLException {
    String sql = "SELECT AVG(rating) AS avg_rating, COUNT(*) AS total FROM diary_ratings WHERE diary_id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return new DiaryRateResponse(resultSet.getDouble("avg_rating"),
              resultSet.getInt("total"));
        }
      }
    }
    return new DiaryRateResponse(0.0, 0);
  }

  private void updateDiaryRating(Connection connection, long diaryId, DiaryRateResponse response)
      throws SQLException {
    String sql = "UPDATE travel_diaries SET average_rating = ?, total_ratings = ? WHERE id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setDouble(1, response.getAverageRating());
      statement.setInt(2, response.getTotalRatings());
      statement.setLong(3, diaryId);
      statement.executeUpdate();
    }
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
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

  private static final class DiaryOwnership {
    private final Long id;
    private final Long userId;

    private DiaryOwnership(Long id, Long userId) {
      this.id = id;
      this.userId = userId;
    }
  }
}
