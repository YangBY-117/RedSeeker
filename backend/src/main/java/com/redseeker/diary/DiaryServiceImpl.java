package com.redseeker.diary;

import com.redseeker.common.ErrorCode;
import com.redseeker.common.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DiaryServiceImpl implements DiaryService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiaryServiceImpl.class);
  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_PAGE_SIZE = 10;

  private final String databaseUrl;

  public DiaryServiceImpl() {
    this.databaseUrl = resolveDatabaseUrl();
  }

  @Override
  public DiaryListResponse listDiaries(DiaryListQuery query) {
    DiaryListQuery normalized = normalizeQuery(query);
    List<DiarySummary> diaries = loadDiarySummaries(normalized.getDestination());
    Map<Long, Double> interestScores = loadUserInterestScores(normalized.getUserId());
    diaries.forEach(diary -> applyRecommendScore(diary, interestScores));
    diaries = sortDiaries(diaries, normalized.getSortBy());
    return paginate(diaries, normalized.getPage(), normalized.getPageSize());
  }

  @Override
  public DiaryListResponse searchByDestination(DiaryListQuery query) {
    DiaryListQuery normalized = normalizeQuery(query);
    if (normalized.getDestination() == null || normalized.getDestination().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "destination is required");
    }
    List<DiarySummary> diaries = loadDiarySummaries(normalized.getDestination());
    diaries = sortDiaries(diaries, normalized.getSortBy());
    return paginate(diaries, normalized.getPage(), normalized.getPageSize());
  }

  @Override
  public DiarySummary searchByTitle(String title) {
    if (title == null || title.isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "title is required");
    }
    String sql =
        "SELECT d.id, d.title, d.content, d.content_compressed, d.destination, d.travel_date, "
            + "d.view_count, d.average_rating, "
            + "d.total_ratings, d.created_at, u.id AS user_id, u.username, "
            + "(SELECT file_path FROM diary_media WHERE diary_id = d.id AND media_type = 'image' "
            + "ORDER BY display_order ASC LIMIT 1) AS cover_image "
            + "FROM travel_diaries d JOIN users u ON u.id = d.user_id WHERE d.title = ? LIMIT 1";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, title.trim());
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return mapDiarySummary(rs);
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to search diary by title", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to search diary");
    }
    return null;
  }

  @Override
  public DiaryListResponse fullTextSearch(String keyword, int page, int pageSize) {
    if (keyword == null || keyword.isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "keyword is required");
    }
    int normalizedPage = page > 0 ? page : DEFAULT_PAGE;
    int normalizedSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
    List<DiarySummary> diaries = new ArrayList<>();
    String sql =
        "SELECT d.id, d.title, d.content, d.content_compressed, d.destination, d.travel_date, "
            + "d.view_count, d.average_rating, "
            + "d.total_ratings, d.created_at, u.id AS user_id, u.username, "
            + "(SELECT file_path FROM diary_media WHERE diary_id = d.id AND media_type = 'image' "
            + "ORDER BY display_order ASC LIMIT 1) AS cover_image "
            + "FROM travel_diaries_fts f JOIN travel_diaries d ON f.rowid = d.id "
            + "JOIN users u ON u.id = d.user_id WHERE travel_diaries_fts MATCH ? ORDER BY rank";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, keyword.trim());
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          diaries.add(mapDiarySummary(rs));
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to full text search diary", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to search diary");
    }
    return paginate(diaries, normalizedPage, normalizedSize);
  }

  @Override
  public DiaryDetailResponse getDiaryDetail(Long diaryId, Long userId) {
    DiaryDetailResponse detail = loadDiaryDetail(diaryId, userId);
    if (detail == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "diary not found");
    }
    incrementDiaryView(diaryId, userId);
    detail.setViewCount(detail.getViewCount() + 1);
    return detail;
  }

  @Override
  public DiarySummary createDiary(DiaryCreateRequest request, Long userId) {
    assertUser(userId);
    
    // 验证必填字段
    if (request.getTitle() == null || request.getTitle().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "标题不能为空");
    }
    if (request.getContent() == null || request.getContent().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "内容不能为空");
    }
    
    String sql =
        "INSERT INTO travel_diaries (user_id, title, content, destination, travel_date, view_count, "
            + "average_rating, total_ratings, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 0, 0, 0, "
            + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    long diaryId;
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setLong(1, userId);
      statement.setString(2, request.getTitle());
      statement.setString(3, request.getContent());
      statement.setString(4, request.getDestination());
      statement.setString(5, request.getTravelDate());
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          diaryId = keys.getLong(1);
          LOGGER.info("日记创建成功: diaryId={}, userId={}, title={}", diaryId, userId, request.getTitle());
        } else {
          LOGGER.error("无法获取生成的日记ID");
          throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to create diary: cannot get generated key");
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("创建日记失败", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to create diary: " + ex.getMessage());
    }

    // 保存媒体文件和关联景点（即使失败也不影响日记创建）
    try {
      saveDiaryMedia(diaryId, request.getImages(), request.getImageUrls(), request.getVideos(), request.getVideoUrls());
      LOGGER.info("日记媒体文件保存成功: diaryId={}", diaryId);
    } catch (Exception ex) {
      LOGGER.warn("保存日记媒体文件失败，但日记已创建: diaryId={}", diaryId, ex);
    }
    
    try {
      saveDiaryAttractions(diaryId, request.getAttractionIds());
      LOGGER.info("日记关联景点保存成功: diaryId={}, count={}", diaryId, 
          request.getAttractionIds() != null ? request.getAttractionIds().size() : 0);
    } catch (Exception ex) {
      LOGGER.warn("保存日记关联景点失败，但日记已创建: diaryId={}", diaryId, ex);
    }
    
    // 返回创建的日记摘要（即使加载失败也返回基本信息，确保前端能收到成功响应）
    try {
      DiarySummary summary = loadDiarySummaryById(diaryId);
      LOGGER.info("日记创建完成: diaryId={}, title={}", diaryId, summary.getTitle());
      return summary;
    } catch (Exception ex) {
      LOGGER.error("加载创建的日记失败，返回基本信息: diaryId={}", diaryId, ex);
      // 即使加载失败，也返回一个基本的摘要，确保前端知道创建成功
      DiarySummary summary = new DiarySummary();
      summary.setId(diaryId);
      summary.setTitle(request.getTitle());
      summary.setDestination(request.getDestination());
      summary.setContent(request.getContent());
      summary.setTravelDate(request.getTravelDate());
      summary.setViewCount(0);
      summary.setAverageRating(0.0);
      summary.setTotalRatings(0);
      // 创建一个基本的作者信息（需要从userId获取，这里先设为null或默认值）
      summary.setAuthor(new DiaryAuthor(userId, "用户"));
      LOGGER.info("返回日记基本信息: diaryId={}", diaryId);
      return summary;
    }
  }

  @Override
  public DiarySummary updateDiary(Long diaryId, DiaryCreateRequest request, Long userId) {
    assertDiaryOwner(diaryId, userId);
    String sql =
        "UPDATE travel_diaries SET title = ?, content = ?, destination = ?, travel_date = ?, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, request.getTitle());
      statement.setString(2, request.getContent());
      statement.setString(3, request.getDestination());
      statement.setString(4, request.getTravelDate());
      statement.setLong(5, diaryId);
      statement.executeUpdate();
    } catch (SQLException ex) {
      LOGGER.error("Failed to update diary", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to update diary");
    }

    replaceDiaryMedia(diaryId, request.getImages(), request.getImageUrls(), request.getVideos(), request.getVideoUrls());
    replaceDiaryAttractions(diaryId, request.getAttractionIds());
    return loadDiarySummaryById(diaryId);
  }

  @Override
  public void deleteDiary(Long diaryId, Long userId) {
    assertDiaryOwner(diaryId, userId);
    try (Connection connection = openConnection()) {
      deleteDiaryMedia(connection, diaryId);
      deleteDiaryAttractions(connection, diaryId);
      deleteDiaryRatings(connection, diaryId);
      deleteDiaryBrowses(connection, diaryId);
      try (PreparedStatement statement = connection.prepareStatement("DELETE FROM travel_diaries WHERE id = ?")) {
        statement.setLong(1, diaryId);
        statement.executeUpdate();
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to delete diary", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to delete diary");
    }
  }

  @Override
  public DiaryRatingResponse rateDiary(Long diaryId, int rating, Long userId) {
    assertUser(userId);
    if (rating < 1 || rating > 5) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "rating must be 1-5");
    }
    try (Connection connection = openConnection()) {
      Long existingId = findDiaryRating(connection, diaryId, userId);
      if (existingId == null) {
        try (PreparedStatement statement =
            connection.prepareStatement(
                "INSERT INTO diary_ratings (diary_id, user_id, rating, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {
          statement.setLong(1, diaryId);
          statement.setLong(2, userId);
          statement.setInt(3, rating);
          statement.executeUpdate();
        }
      } else {
        try (PreparedStatement statement =
            connection.prepareStatement("UPDATE diary_ratings SET rating = ? WHERE id = ?")) {
          statement.setInt(1, rating);
          statement.setLong(2, existingId);
          statement.executeUpdate();
        }
      }
      DiaryRatingResponse response = refreshDiaryRating(connection, diaryId);
      return response;
    } catch (SQLException ex) {
      LOGGER.error("Failed to rate diary", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to rate diary");
    }
  }

  @Override
  public DiaryAnimationResponse generateAnimation(Long diaryId, DiaryAnimationRequest request, Long userId) {
    assertUser(userId);
    // Ensure diary exists before creating a task.
    loadDiarySummaryById(diaryId);
    String taskId = UUID.randomUUID().toString();
    int imageCount = request != null && request.getImages() != null ? request.getImages().size() : 0;
    LOGGER.info("Generate animation requested: diaryId={}, userId={}, images={}", diaryId, userId, imageCount);
    return new DiaryAnimationResponse(taskId, "processing", 30);
  }

  @Override
  public DiaryAnimationStatusResponse getAnimationStatus(String taskId) {
    // 调用阿里云API查询任务状态
    String queryUrl = "https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId;
    try {
      java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
      java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
          .uri(java.net.URI.create(queryUrl))
          .header("Authorization", "Bearer sk-7c644f6974a04921a2f513c43bba5d18")
          .GET()
          .build();
      
      java.net.http.HttpResponse<String> response = httpClient.send(request, 
          java.net.http.HttpResponse.BodyHandlers.ofString());
      
      if (response.statusCode() != 200) {
        LOGGER.error("查询动画状态失败: status={}, body={}", response.statusCode(), response.body());
        return new DiaryAnimationStatusResponse(taskId, "failed", null);
      }
      
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response.body());
      
      com.fasterxml.jackson.databind.JsonNode output = root.path("output");
      String taskStatus = output.path("task_status").asText("UNKNOWN").toLowerCase();
      String videoUrl = null;
      
      if ("succeeded".equals(taskStatus)) {
        videoUrl = output.path("video_url").asText();
      }
      
      LOGGER.info("动画状态查询: taskId={}, status={}, hasVideo={}", taskId, taskStatus, videoUrl != null);
      return new DiaryAnimationStatusResponse(taskId, taskStatus, videoUrl);
    } catch (Exception ex) {
      LOGGER.error("查询动画状态异常: taskId={}", taskId, ex);
      return new DiaryAnimationStatusResponse(taskId, "unknown", null);
    }
  }

  private DiaryListQuery normalizeQuery(DiaryListQuery query) {
    DiaryListQuery normalized = new DiaryListQuery();
    normalized.setSortBy(query.getSortBy());
    normalized.setDestination(query.getDestination());
    normalized.setUserId(query.getUserId());
    normalized.setPage(query.getPage() > 0 ? query.getPage() : DEFAULT_PAGE);
    normalized.setPageSize(query.getPageSize() > 0 ? query.getPageSize() : DEFAULT_PAGE_SIZE);
    return normalized;
  }

  private List<DiarySummary> loadDiarySummaries(String destination) {
    List<DiarySummary> diaries = new ArrayList<>();
    String sql =
        "SELECT d.id, d.title, d.content, d.content_compressed, d.destination, d.travel_date, "
            + "d.view_count, d.average_rating, "
            + "d.total_ratings, d.created_at, u.id AS user_id, u.username, "
            + "(SELECT file_path FROM diary_media WHERE diary_id = d.id AND media_type = 'image' "
            + "ORDER BY display_order ASC LIMIT 1) AS cover_image "
            + "FROM travel_diaries d JOIN users u ON u.id = d.user_id";
    boolean filterDestination = destination != null && !destination.isBlank();
    if (filterDestination) {
      sql += " WHERE d.destination LIKE ?";
    }
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      if (filterDestination) {
        statement.setString(1, "%" + destination.trim() + "%");
      }
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          diaries.add(mapDiarySummary(rs));
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to load diaries", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to load diaries");
    }
    return diaries;
  }

  private DiarySummary mapDiarySummary(ResultSet rs) throws SQLException {
    DiarySummary summary = new DiarySummary();
    summary.setId(rs.getLong("id"));
    summary.setTitle(rs.getString("title"));
    summary.setContent(extractContent(rs));
    summary.setDestination(rs.getString("destination"));
    summary.setTravelDate(rs.getString("travel_date"));
    summary.setViewCount(rs.getInt("view_count"));
    
    // 处理可能为null的字段
    double avgRating = rs.getDouble("average_rating");
    if (rs.wasNull()) {
      summary.setAverageRating(null);
    } else {
      summary.setAverageRating(avgRating);
    }
    
    int totalRatings = rs.getInt("total_ratings");
    if (rs.wasNull()) {
      summary.setTotalRatings(null);
    } else {
      summary.setTotalRatings(totalRatings);
    }
    
    // 处理created_at字段（可能是DATETIME类型）
    Object createdAtObj = rs.getObject("created_at");
    if (createdAtObj != null) {
      summary.setCreatedAt(createdAtObj.toString());
    } else {
      summary.setCreatedAt(null);
    }
    
    summary.setCoverImage(rs.getString("cover_image"));
    summary.setAuthor(new DiaryAuthor(rs.getLong("user_id"), rs.getString("username")));
    return summary;
  }

  private DiaryListResponse paginate(List<DiarySummary> diaries, int page, int pageSize) {
    int total = diaries.size();
    int totalPages = (int) Math.ceil(total / (double) pageSize);
    int fromIndex = Math.min((page - 1) * pageSize, total);
    int toIndex = Math.min(fromIndex + pageSize, total);

    DiaryListResponse response = new DiaryListResponse();
    response.setDiaries(diaries.subList(fromIndex, toIndex));
    response.setTotal(total);
    response.setPage(page);
    response.setPageSize(pageSize);
    response.setTotalPages(totalPages);
    return response;
  }

  private List<DiarySummary> sortDiaries(List<DiarySummary> diaries, String sortBy) {
    String mode = sortBy == null ? "heat" : sortBy.trim().toLowerCase();
    Comparator<DiarySummary> comparator = Comparator.comparingInt(DiarySummary::getViewCount).reversed();
    if ("rating".equals(mode)) {
      comparator =
          Comparator.comparing(
                  (DiarySummary diary) -> diary.getAverageRating() == null ? 0 : diary.getAverageRating())
              .reversed();
    } else if ("time".equals(mode)) {
      comparator = Comparator.comparing(DiarySummary::getCreatedAt).reversed();
    }
    return diaries.stream().sorted(comparator).collect(Collectors.toList());
  }

  private void applyRecommendScore(DiarySummary diary, Map<Long, Double> interestScores) {
    double heatScore = diary.getViewCount();
    double ratingScore = diary.getAverageRating() != null ? diary.getAverageRating() * 20 : 0;
    double interestScore = interestScores.getOrDefault(diary.getId(), 0.0);
    double recommend = heatScore * 0.4 + ratingScore * 0.4 + interestScore * 0.2;
    diary.setAverageRating(
        diary.getAverageRating() == null ? 0.0 : diary.getAverageRating());
    diary.setTotalRatings(
        diary.getTotalRatings() == null ? 0 : diary.getTotalRatings());
  }

  private Map<Long, Double> loadUserInterestScores(Long userId) {
    if (userId == null) {
      return new HashMap<>();
    }
    Map<Long, Double> scores = new HashMap<>();
    String sql =
        "SELECT da.diary_id, COUNT(*) AS hits "
            + "FROM diary_attractions da "
            + "JOIN user_browse_history ub ON ub.attraction_id = da.attraction_id "
            + "WHERE ub.user_id = ? GROUP BY da.diary_id";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, userId);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          scores.put(rs.getLong("diary_id"), rs.getDouble("hits") * 10);
        }
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to load interest scores", ex);
    }
    return scores;
  }

  private DiaryDetailResponse loadDiaryDetail(Long diaryId, Long userId) {
    String sql =
        "SELECT d.id, d.title, d.content, d.content_compressed, d.destination, d.travel_date, d.view_count, "
            + "d.average_rating, d.total_ratings, d.created_at, d.updated_at, u.id AS user_id, u.username "
            + "FROM travel_diaries d JOIN users u ON u.id = d.user_id WHERE d.id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          DiaryDetailResponse detail = new DiaryDetailResponse();
          detail.setId(rs.getLong("id"));
          detail.setTitle(rs.getString("title"));
          detail.setContent(extractContent(rs));
          detail.setDestination(rs.getString("destination"));
          detail.setTravelDate(rs.getString("travel_date"));
          detail.setViewCount(rs.getInt("view_count"));
          detail.setAverageRating(rs.getDouble("average_rating"));
          detail.setTotalRatings(rs.getInt("total_ratings"));
          detail.setCreatedAt(rs.getString("created_at"));
          detail.setUpdatedAt(rs.getString("updated_at"));
          detail.setAuthor(new DiaryAuthor(rs.getLong("user_id"), rs.getString("username")));
          detail.setMedia(loadDiaryMedia(connection, diaryId));
          detail.setAttractions(loadDiaryAttractions(connection, diaryId));
          detail.setUserRating(loadUserDiaryRating(connection, diaryId, userId));
          return detail;
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to load diary detail", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to load diary");
    }
    return null;
  }

  private List<DiaryMedia> loadDiaryMedia(Connection connection, Long diaryId) throws SQLException {
    List<DiaryMedia> media = new ArrayList<>();
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT id, media_type, file_path, thumbnail_path, display_order FROM diary_media WHERE diary_id = ? "
                + "ORDER BY display_order ASC")) {
      statement.setLong(1, diaryId);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          media.add(
              new DiaryMedia(
                  rs.getLong("id"),
                  rs.getString("media_type"),
                  rs.getString("file_path"),
                  rs.getString("thumbnail_path"),
                  rs.getInt("display_order")));
        }
      }
    }
    return media;
  }

  private List<DiaryAttraction> loadDiaryAttractions(Connection connection, Long diaryId) throws SQLException {
    List<DiaryAttraction> attractions = new ArrayList<>();
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT a.id, a.name, a.address FROM diary_attractions da "
                + "JOIN attractions a ON a.id = da.attraction_id WHERE da.diary_id = ?")) {
      statement.setLong(1, diaryId);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          attractions.add(new DiaryAttraction(rs.getLong("id"), rs.getString("name"), rs.getString("address")));
        }
      }
    }
    return attractions;
  }

  private Integer loadUserDiaryRating(Connection connection, Long diaryId, Long userId) throws SQLException {
    if (userId == null) {
      return null;
    }
    try (PreparedStatement statement =
        connection.prepareStatement("SELECT rating FROM diary_ratings WHERE diary_id = ? AND user_id = ?")) {
      statement.setLong(1, diaryId);
      statement.setLong(2, userId);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("rating");
        }
      }
    }
    return null;
  }

  private DiarySummary loadDiarySummaryById(long diaryId) {
    String sql =
        "SELECT d.id, d.title, d.content, d.content_compressed, d.destination, d.travel_date, "
            + "d.view_count, d.average_rating, "
            + "d.total_ratings, d.created_at, u.id AS user_id, u.username, "
            + "(SELECT file_path FROM diary_media WHERE diary_id = d.id AND media_type = 'image' "
            + "ORDER BY display_order ASC LIMIT 1) AS cover_image "
            + "FROM travel_diaries d JOIN users u ON u.id = d.user_id WHERE d.id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return mapDiarySummary(rs);
        } else {
          LOGGER.warn("日记不存在: diaryId={}", diaryId);
          throw new ServiceException(ErrorCode.NOT_FOUND, "diary not found");
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("加载日记摘要失败: diaryId={}", diaryId, ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to load diary: " + ex.getMessage());
    }
  }

  private void incrementDiaryView(Long diaryId, Long userId) {
    try (Connection connection = openConnection()) {
      try (PreparedStatement statement =
          connection.prepareStatement("UPDATE travel_diaries SET view_count = view_count + 1 WHERE id = ?")) {
        statement.setLong(1, diaryId);
        statement.executeUpdate();
      }
      try (PreparedStatement statement =
          connection.prepareStatement(
              "INSERT INTO diary_browse_history (diary_id, user_id, browse_time) VALUES (?, ?, CURRENT_TIMESTAMP)")) {
        statement.setLong(1, diaryId);
        if (userId == null) {
          statement.setObject(2, null);
        } else {
          statement.setLong(2, userId);
        }
        statement.executeUpdate();
      }
    } catch (SQLException ex) {
      LOGGER.warn("Failed to increment diary view", ex);
    }
  }

  private void saveDiaryMedia(
      long diaryId,
      List<MultipartFile> images,
      List<String> imageUrls,
      List<MultipartFile> videos,
      List<String> videoUrls) {
    if ((images == null || images.isEmpty())
        && (videos == null || videos.isEmpty())
        && (imageUrls == null || imageUrls.isEmpty())
        && (videoUrls == null || videoUrls.isEmpty())) {
      return;
    }
    try (Connection connection = openConnection()) {
      int order = 0;
      order = insertMedia(connection, diaryId, images, "image", order);
      order = insertUrlMedia(connection, diaryId, imageUrls, "image", order);
      order = insertMedia(connection, diaryId, videos, "video", order);
      insertUrlMedia(connection, diaryId, videoUrls, "video", order);
    } catch (SQLException ex) {
      LOGGER.error("Failed to save diary media", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to save diary media");
    }
  }

  private void replaceDiaryMedia(
      long diaryId,
      List<MultipartFile> images,
      List<String> imageUrls,
      List<MultipartFile> videos,
      List<String> videoUrls) {
    try (Connection connection = openConnection()) {
      deleteDiaryMedia(connection, diaryId);
      int order = 0;
      order = insertMedia(connection, diaryId, images, "image", order);
      order = insertUrlMedia(connection, diaryId, imageUrls, "image", order);
      order = insertMedia(connection, diaryId, videos, "video", order);
      insertUrlMedia(connection, diaryId, videoUrls, "video", order);
    } catch (SQLException ex) {
      LOGGER.error("Failed to replace diary media", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to save diary media");
    }
  }

  private int insertMedia(
      Connection connection, long diaryId, List<MultipartFile> files, String type, int startOrder)
      throws SQLException {
    if (files == null) {
      return startOrder;
    }
    int order = startOrder;
    for (MultipartFile file : files) {
      if (file == null || file.isEmpty()) {
        continue;
      }
      String storedPath = storeFile(file, diaryId, type, order);
      try (PreparedStatement statement =
          connection.prepareStatement(
              "INSERT INTO diary_media (diary_id, media_type, file_path, display_order, created_at) "
                  + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)")) {
        statement.setLong(1, diaryId);
        statement.setString(2, type);
        statement.setString(3, storedPath);
        statement.setInt(4, order);
        statement.executeUpdate();
      }
      order++;
    }
    return order;
  }

  private int insertUrlMedia(
      Connection connection, long diaryId, List<String> urls, String type, int startOrder)
      throws SQLException {
    if (urls == null) {
      return startOrder;
    }
    int order = startOrder;
    for (String url : urls) {
      if (url == null || url.isBlank()) {
        continue;
      }
      try (PreparedStatement statement =
          connection.prepareStatement(
              "INSERT INTO diary_media (diary_id, media_type, file_path, display_order, created_at) "
                  + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)")) {
        statement.setLong(1, diaryId);
        statement.setString(2, type);
        statement.setString(3, url.trim());
        statement.setInt(4, order);
        statement.executeUpdate();
      }
      order++;
    }
    return order;
  }

  private void deleteDiaryMedia(Connection connection, long diaryId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("DELETE FROM diary_media WHERE diary_id = ?")) {
      statement.setLong(1, diaryId);
      statement.executeUpdate();
    }
  }

  private void saveDiaryAttractions(long diaryId, List<Long> attractionIds) {
    if (attractionIds == null || attractionIds.isEmpty()) {
      return;
    }
    try (Connection connection = openConnection()) {
      insertDiaryAttractions(connection, diaryId, attractionIds);
    } catch (SQLException ex) {
      LOGGER.error("Failed to save diary attractions", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to save diary attractions");
    }
  }

  private void replaceDiaryAttractions(long diaryId, List<Long> attractionIds) {
    try (Connection connection = openConnection()) {
      deleteDiaryAttractions(connection, diaryId);
      insertDiaryAttractions(connection, diaryId, attractionIds);
    } catch (SQLException ex) {
      LOGGER.error("Failed to replace diary attractions", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to save diary attractions");
    }
  }

  private void insertDiaryAttractions(Connection connection, long diaryId, List<Long> attractionIds)
      throws SQLException {
    if (attractionIds == null) {
      return;
    }
    for (Long attractionId : attractionIds) {
      if (attractionId == null) {
        continue;
      }
      try (PreparedStatement statement =
          connection.prepareStatement("INSERT INTO diary_attractions (diary_id, attraction_id) VALUES (?, ?)")) {
        statement.setLong(1, diaryId);
        statement.setLong(2, attractionId);
        statement.executeUpdate();
      }
    }
  }

  private void deleteDiaryAttractions(Connection connection, long diaryId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("DELETE FROM diary_attractions WHERE diary_id = ?")) {
      statement.setLong(1, diaryId);
      statement.executeUpdate();
    }
  }

  private void deleteDiaryRatings(Connection connection, long diaryId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("DELETE FROM diary_ratings WHERE diary_id = ?")) {
      statement.setLong(1, diaryId);
      statement.executeUpdate();
    }
  }

  private void deleteDiaryBrowses(Connection connection, long diaryId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("DELETE FROM diary_browse_history WHERE diary_id = ?")) {
      statement.setLong(1, diaryId);
      statement.executeUpdate();
    }
  }

  private Long findDiaryRating(Connection connection, Long diaryId, Long userId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("SELECT id FROM diary_ratings WHERE diary_id = ? AND user_id = ?")) {
      statement.setLong(1, diaryId);
      statement.setLong(2, userId);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("id");
        }
      }
    }
    return null;
  }

  private DiaryRatingResponse refreshDiaryRating(Connection connection, Long diaryId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT AVG(rating) AS avg_rating, COUNT(*) AS total FROM diary_ratings WHERE diary_id = ?")) {
      statement.setLong(1, diaryId);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          double average = rs.getDouble("avg_rating");
          int total = rs.getInt("total");
          try (PreparedStatement update =
              connection.prepareStatement(
                  "UPDATE travel_diaries SET average_rating = ?, total_ratings = ? WHERE id = ?")) {
            update.setDouble(1, average);
            update.setInt(2, total);
            update.setLong(3, diaryId);
            update.executeUpdate();
          }
          return new DiaryRatingResponse(average, total);
        }
      }
    }
    return new DiaryRatingResponse(0.0, 0);
  }

  private void assertDiaryOwner(Long diaryId, Long userId) {
    assertUser(userId);
    String sql = "SELECT user_id FROM travel_diaries WHERE id = ?";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, diaryId);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          long ownerId = rs.getLong("user_id");
          if (ownerId != userId) {
            throw new ServiceException(ErrorCode.FORBIDDEN, "not allowed");
          }
          return;
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to verify diary owner", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to verify diary owner");
    }
    throw new ServiceException(ErrorCode.NOT_FOUND, "diary not found");
  }

  private void assertUser(Long userId) {
    if (userId == null) {
      throw new ServiceException(ErrorCode.UNAUTHORIZED, "Authorization required");
    }
  }

  private String extractContent(ResultSet rs) throws SQLException {
    String content = rs.getString("content");
    if (content != null && !content.isBlank()) {
      return content;
    }
    byte[] compressed = rs.getBytes("content_compressed");
    if (compressed == null || compressed.length == 0) {
      return content;
    }
    try (ByteArrayInputStream input = new ByteArrayInputStream(compressed);
        GZIPInputStream gzip = new GZIPInputStream(input);
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      gzip.transferTo(output);
      return output.toString(StandardCharsets.UTF_8);
    } catch (IOException ex) {
      LOGGER.warn("Failed to decompress diary content", ex);
      return content;
    }
  }

  private String storeFile(MultipartFile file, long diaryId, String type, int order) {
    String extension = resolveExtension(file.getOriginalFilename());
    String filename = "diary_" + diaryId + "_" + type + "_" + order + extension;
    Path baseDir = Paths.get("uploads", "diaries", String.valueOf(diaryId));
    try {
      Files.createDirectories(baseDir);
      Path target = baseDir.resolve(filename);
      Files.write(target, file.getBytes());
      return baseDir.resolve(filename).toString().replace("\\", "/");
    } catch (IOException ex) {
      LOGGER.error("Failed to store diary media", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to store media");
    }
  }

  private String resolveExtension(String filename) {
    if (filename == null) {
      return "";
    }
    int index = filename.lastIndexOf('.');
    return index >= 0 ? filename.substring(index) : "";
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
}
