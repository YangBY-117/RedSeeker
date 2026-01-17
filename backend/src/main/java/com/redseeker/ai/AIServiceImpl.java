package com.redseeker.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redseeker.common.ErrorCode;
import com.redseeker.common.ServiceException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AIServiceImpl implements AIService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AIServiceImpl.class);
  
  // 阿里云通义千问API配置
  private static final String ALIYUN_API_KEY = "sk-7c644f6974a04921a2f513c43bba5d18";
  private static final String TEXT_TO_IMAGE_MODEL = "qwen-image-max"; // 推荐使用max模型
  private static final String IMAGE_TO_VIDEO_MODEL = "wan2.6-i2v";
  // 同步接口（推荐）- 使用multimodal-generation接口
  private static final String TEXT_TO_IMAGE_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
  private static final String IMAGE_TO_VIDEO_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis";
  
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final com.redseeker.recommend.AiService textAiService; // 用于生成日记内容
  private final String databaseUrl;

  public AIServiceImpl(com.redseeker.recommend.AiService textAiService) {
    this.textAiService = textAiService;
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
    this.databaseUrl = resolveDatabaseUrl();
    LOGGER.info("AIService初始化完成，使用阿里云API密钥: {}", ALIYUN_API_KEY.substring(0, 10) + "***");
  }

  @Override
  public DiaryGenerateResponse generateDiaryContent(DiaryGenerateRequest request) {
    try {
      String prompt = buildDiaryPrompt(request);
      String content = textAiService.generateContent(prompt);
      
      DiaryGenerateResponse response = new DiaryGenerateResponse();
      response.setContent(content);
      response.setTitle(extractTitle(content));
      return response;
    } catch (Exception ex) {
      LOGGER.error("生成日记内容失败", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "生成日记内容失败: " + ex.getMessage());
    }
  }

  @Override
  public ImageGenerateResponse generateImageFromText(TextToImageRequest request) {
    try {
      // 构建请求体 - 使用同步接口格式
      Map<String, Object> inputMap = new HashMap<>();
      List<Map<String, Object>> messages = new ArrayList<>();
      Map<String, Object> message = new HashMap<>();
      message.put("role", "user");
      List<Map<String, Object>> content = new ArrayList<>();
      Map<String, Object> textContent = new HashMap<>();
      textContent.put("text", request.getPrompt());
      content.add(textContent);
      message.put("content", content);
      messages.add(message);
      inputMap.put("messages", messages);
      
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("size", "1664*928"); // 默认分辨率
      parameters.put("negative_prompt", "低分辨率，低画质，肢体畸形，手指畸形，画面过饱和，蜡像感，人脸无细节，过度光滑，画面具有AI感。构图混乱。文字模糊，扭曲。");
      parameters.put("prompt_extend", true);
      parameters.put("watermark", false);
      
      Map<String, Object> body = new HashMap<>();
      body.put("model", TEXT_TO_IMAGE_MODEL);
      body.put("input", inputMap);
      body.put("parameters", parameters);

      String jsonBody = objectMapper.writeValueAsString(body);
      LOGGER.info("文生图请求: model={}, prompt长度={}", TEXT_TO_IMAGE_MODEL, request.getPrompt().length());
      
      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create(TEXT_TO_IMAGE_API_URL))
          .header("Authorization", "Bearer " + ALIYUN_API_KEY)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
          .build();

      HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      
      LOGGER.info("文生图API响应: status={}", response.statusCode());
      
      if (response.statusCode() != 200) {
        LOGGER.error("阿里云文生图API调用失败: status={}, body={}", response.statusCode(), response.body());
        throw new ServiceException(ErrorCode.INTERNAL_ERROR, "文生图API调用失败: HTTP " + response.statusCode());
      }

      JsonNode root = objectMapper.readTree(response.body());
      
      // 检查是否有错误
      if (root.has("code") && !root.path("code").asText().isEmpty()) {
        String errorMsg = root.path("message").asText("未知错误");
        LOGGER.error("文生图API返回错误: code={}, message={}, body={}", 
            root.path("code").asText(), errorMsg, response.body());
        throw new ServiceException(ErrorCode.INTERNAL_ERROR, "文生图API返回错误: " + errorMsg);
      }
      
      // 检查输出格式 - 同步接口返回格式：output.choices[0].message.content[0].image
      JsonNode output = root.path("output");
      if (output.isMissingNode()) {
        LOGGER.error("文生图API返回格式错误，缺少output字段: {}", response.body());
        throw new ServiceException(ErrorCode.INTERNAL_ERROR, "文生图API返回格式错误：缺少output");
      }
      
      JsonNode choices = output.path("choices");
      if (!choices.isArray() || choices.size() == 0) {
        LOGGER.error("文生图API返回格式错误，choices为空: {}", response.body());
        throw new ServiceException(ErrorCode.INTERNAL_ERROR, "文生图API返回格式错误：choices为空");
      }

      JsonNode firstChoice = choices.get(0);
      JsonNode messageNode = firstChoice.path("message");
      JsonNode contentArray = messageNode.path("content");
      
      if (!contentArray.isArray() || contentArray.size() == 0) {
        LOGGER.error("文生图API返回格式错误，content为空: {}", response.body());
        throw new ServiceException(ErrorCode.INTERNAL_ERROR, "文生图API返回格式错误：content为空");
      }
      
      JsonNode imageContent = contentArray.get(0);
      String imageUrl = imageContent.path("image").asText();
      
      if (imageUrl == null || imageUrl.isBlank()) {
        LOGGER.error("文生图API返回格式错误，无法获取图片URL: {}", response.body());
        throw new ServiceException(ErrorCode.INTERNAL_ERROR, "文生图API返回格式错误：无法获取图片URL");
      }
      
      ImageGenerateResponse result = new ImageGenerateResponse();
      result.setImageUrl(imageUrl);
      result.setTaskId(UUID.randomUUID().toString());
      LOGGER.info("文生图成功: imageUrl={}", imageUrl.substring(0, Math.min(100, imageUrl.length())));
      return result;
    } catch (IOException | InterruptedException ex) {
      LOGGER.error("文生图失败", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "文生图失败: " + ex.getMessage());
    }
  }

  @Override
  public AnimationGenerateResponse generateAnimationFromImages(ImageToAnimationRequest request) {
    try {
      if (request.getImages() == null || request.getImages().isEmpty()) {
        throw new ServiceException(ErrorCode.VALIDATION_ERROR, "至少需要一张图片");
      }

      // 使用第一张图片
      String imageUrl = request.getImages().get(0);
      
      // 构建input对象 - 根据API文档，img_url支持URL或base64格式
      Map<String, Object> inputMap = new HashMap<>();
      
      // img_url支持：
      // 1. 公网URL（http://或https://）
      // 2. Base64编码（data:image/png;base64,xxx格式）
      if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
        // 公网URL
        inputMap.put("img_url", imageUrl);
      } else if (imageUrl.startsWith("data:image")) {
        // Base64格式（data:image/png;base64,xxx）
        inputMap.put("img_url", imageUrl);
      } else {
        // 可能是纯base64字符串，需要转换为data URL格式
        inputMap.put("img_url", "data:image/png;base64," + imageUrl);
      }
      
      // 添加prompt（可选）
      if (request.getDescription() != null && !request.getDescription().isBlank()) {
        inputMap.put("prompt", request.getDescription());
      }
      
      // 构建parameters对象
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("resolution", "720P"); // 默认720P，可选720P、1080P
      parameters.put("duration", 5); // 默认5秒，可选5、10、15
      parameters.put("prompt_extend", true); // 开启智能改写
      parameters.put("watermark", false); // 不添加水印
      
      Map<String, Object> body = new HashMap<>();
      body.put("model", IMAGE_TO_VIDEO_MODEL);
      body.put("input", inputMap);
      body.put("parameters", parameters);
      
      LOGGER.info("图生视频请求: model={}, imageUrl长度={}, hasPrompt={}", 
          IMAGE_TO_VIDEO_MODEL, imageUrl.length(), request.getDescription() != null);

      String jsonBody = objectMapper.writeValueAsString(body);
      
      // 根据API文档，异步接口需要添加 X-DashScope-Async: enable 头
      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create(IMAGE_TO_VIDEO_API_URL))
          .header("Authorization", "Bearer " + ALIYUN_API_KEY)
          .header("Content-Type", "application/json")
          .header("X-DashScope-Async", "enable") // 异步接口必需
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
          .build();

      HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      
      LOGGER.info("图生视频API响应: status={}", response.statusCode());
      
      if (response.statusCode() != 200) {
        LOGGER.error("阿里云图生视频API调用失败: status={}, body={}", response.statusCode(), response.body());
        throw new ServiceException(ErrorCode.INTERNAL_ERROR, "图生视频API调用失败: HTTP " + response.statusCode());
      }

      JsonNode root = objectMapper.readTree(response.body());
      
      // 检查是否有错误
      if (root.has("code") && !root.path("code").asText().isEmpty()) {
        String errorMsg = root.path("message").asText("未知错误");
        LOGGER.error("图生视频API返回错误: code={}, message={}, body={}", 
            root.path("code").asText(), errorMsg, response.body());
        throw new ServiceException(ErrorCode.INTERNAL_ERROR, "图生视频API返回错误: " + errorMsg);
      }
      
      JsonNode output = root.path("output");
      if (output.isMissingNode()) {
        LOGGER.error("图生视频API返回格式错误，缺少output字段: {}", response.body());
        throw new ServiceException(ErrorCode.INTERNAL_ERROR, "图生视频API返回格式错误：缺少output");
      }
      
      String taskId = output.path("task_id").asText();
      String taskStatus = output.path("task_status").asText("PENDING");
      
      if (taskId == null || taskId.isBlank()) {
        LOGGER.error("图生视频API返回格式错误，task_id为空: {}", response.body());
        throw new ServiceException(ErrorCode.INTERNAL_ERROR, "图生视频API返回格式错误：task_id为空");
      }
      
      AnimationGenerateResponse result = new AnimationGenerateResponse();
      result.setTaskId(taskId);
      result.setStatus(taskStatus.toLowerCase()); // 转换为小写：pending, running, succeeded, failed
      LOGGER.info("图生视频任务创建成功: taskId={}, status={}", taskId, taskStatus);
      return result;
    } catch (IOException | InterruptedException ex) {
      LOGGER.error("图生视频失败", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "图生视频失败: " + ex.getMessage());
    }
  }

  private String buildDiaryPrompt(DiaryGenerateRequest request) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("请帮我写一篇红色旅游日记。");
    if (request.getPrompt() != null && !request.getPrompt().isBlank()) {
      prompt.append("用户描述：").append(request.getPrompt());
    }
    if (request.getDestination() != null && !request.getDestination().isBlank()) {
      prompt.append("目的地：").append(request.getDestination());
    }
    if (request.getTravelDate() != null && !request.getTravelDate().isBlank()) {
      prompt.append("旅游日期：").append(request.getTravelDate());
    }
    List<HistoricalEventSnapshot> events = loadHistoricalEvents(request);
    if (!events.isEmpty()) {
      prompt.append("以下是相关历史事件与景点背景，请综合这些历史事件进行写作，体现时间脉络与感悟：");
      for (HistoricalEventSnapshot event : events) {
        prompt.append("\n- ")
            .append(event.attractionName());
        if (event.period() != null && !event.period().isBlank()) {
          prompt.append("（").append(event.period()).append("）");
        }
        if (event.eventName() != null && !event.eventName().isBlank()) {
          prompt.append(event.eventName());
        }
        if (event.startYear() != null || event.endYear() != null) {
          prompt.append("【")
              .append(event.startYear() != null ? event.startYear() : "?")
              .append("~")
              .append(event.endYear() != null ? event.endYear() : "?")
              .append("】");
        }
        if (event.description() != null && !event.description().isBlank()) {
          prompt.append("：").append(event.description());
        }
      }
    }
    prompt.append("请写一篇真实、感人的红色旅游日记，包含对革命历史的感悟。");
    return prompt.toString();
  }

  private String extractTitle(String content) {
    // 简单提取：取第一句话或前30个字符
    if (content == null || content.isBlank()) {
      return "红色旅游日记";
    }
    String firstLine = content.split("\n")[0].trim();
    if (firstLine.length() > 30) {
      return firstLine.substring(0, 30) + "...";
    }
    return firstLine;
  }

  private List<HistoricalEventSnapshot> loadHistoricalEvents(DiaryGenerateRequest request) {
    List<Long> attractionIds = new ArrayList<>();
    if (request.getAttractionIds() != null) {
      for (Long id : request.getAttractionIds()) {
        if (id != null && id > 0) {
          attractionIds.add(id);
        }
      }
    }

    if (attractionIds.isEmpty() && request.getDestination() != null && !request.getDestination().isBlank()) {
      attractionIds = loadAttractionIdsByDestination(request.getDestination());
    }

    if (attractionIds.isEmpty()) {
      return List.of();
    }

    String placeholders = attractionIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("");
    String sql =
        "SELECT a.name AS attraction_name, he.event_name, he.period, he.start_year, he.end_year, he.description "
            + "FROM attractions a "
            + "LEFT JOIN attraction_events ae ON ae.attraction_id = a.id "
            + "LEFT JOIN historical_events he ON he.id = ae.event_id "
            + "WHERE a.id IN (" + placeholders + ") "
            + "ORDER BY he.start_year ASC";

    List<HistoricalEventSnapshot> events = new ArrayList<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < attractionIds.size(); i++) {
        statement.setLong(i + 1, attractionIds.get(i));
      }
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          String attractionName = rs.getString("attraction_name");
          String eventName = rs.getString("event_name");
          String period = rs.getString("period");
          Integer startYear = rs.getObject("start_year", Integer.class);
          Integer endYear = rs.getObject("end_year", Integer.class);
          String description = rs.getString("description");
          if ((eventName == null || eventName.isBlank())
              && (description == null || description.isBlank())) {
            continue;
          }
          events.add(new HistoricalEventSnapshot(
              attractionName,
              eventName,
              period,
              startYear,
              endYear,
              description));
        }
      }
    } catch (SQLException ex) {
      LOGGER.warn("加载历史事件失败", ex);
    }

    return events;
  }

  private List<Long> loadAttractionIdsByDestination(String destination) {
    String sql = "SELECT id FROM attractions WHERE name LIKE ? OR address LIKE ? LIMIT 5";
    List<Long> ids = new ArrayList<>();
    String pattern = "%" + destination.trim() + "%";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, pattern);
      statement.setString(2, pattern);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          ids.add(rs.getLong("id"));
        }
      }
    } catch (SQLException ex) {
      LOGGER.warn("根据目的地加载景点失败: {}", destination, ex);
    }
    return ids;
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

  private record HistoricalEventSnapshot(
      String attractionName,
      String eventName,
      String period,
      Integer startYear,
      Integer endYear,
      String description) {}
}
