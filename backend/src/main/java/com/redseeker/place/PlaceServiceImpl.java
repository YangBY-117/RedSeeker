package com.redseeker.place;

// ...existing code...

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlaceServiceImpl implements PlaceService {
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PlaceServiceImpl.class);
  private static final String DEFAULT_TRANSPORT = "walking";

  private static final String AMAP_KEY = "2039f165180b1ece6c8cfb1ae448339b"; // 后端高德API Key
  
  private final String amapKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public PlaceServiceImpl() {
    this.amapKey = resolveAmapKey();
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public Map<String, Object> searchNearbyPlaces(PlaceAroundRequest req) {
    Map<String, Object> result = new HashMap<>();
    try {
      // 构建查询参数
      Map<String, String> queryParams = new LinkedHashMap<>();
      queryParams.put("key", amapKey);
      queryParams.put("location", req.getLongitude() + "," + req.getLatitude());
      
      // 只有当keywords不为空时才添加
      if (req.getKeywords() != null && !req.getKeywords().trim().isEmpty()) {
        queryParams.put("keywords", req.getKeywords().trim());
      }
      
      // 只有当types不为空时才添加
      if (req.getTypes() != null && !req.getTypes().trim().isEmpty()) {
        queryParams.put("types", req.getTypes().trim());
      }
      
      queryParams.put("radius", req.getRadius() == null ? "3000" : req.getRadius().toString());
      queryParams.put("page", req.getPage() == null ? "1" : req.getPage().toString());
      queryParams.put("offset", req.getPageSize() == null ? "20" : req.getPageSize().toString());
      queryParams.put("extensions", "all");
      
      String url = "https://restapi.amap.com/v3/place/around?" + buildQuery(queryParams);
      LOGGER.info("高德API请求: location=({},{}), keywords={}, types={}, radius={}", 
          req.getLongitude(), req.getLatitude(), 
          req.getKeywords() != null ? req.getKeywords() : "", 
          req.getTypes() != null ? req.getTypes() : "",
          req.getRadius() != null ? req.getRadius() : 3000);
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        result.put("success", false);
        result.put("message", "AMap API error: " + response.statusCode());
        return result;
      }
      String responseBody = response.body();
      LOGGER.debug("高德API响应: {}", responseBody);
      JsonNode root = objectMapper.readTree(responseBody);
      String status = root.path("status").asText();
      if (!"1".equals(status)) {
        String info = root.path("info").asText();
        String count = root.path("count").asText();
        LOGGER.warn("高德API返回错误: status={}, info={}, count={}", status, info, count);
        result.put("success", false);
        result.put("message", info.isEmpty() ? "高德API调用失败" : info);
        return result;
      }
      // 转换 POI 列表
      List<Map<String, Object>> places = new ArrayList<>();
      JsonNode poisNode = root.path("pois");
      int totalCount = root.path("count").asInt(0);
      LOGGER.info("高德API返回: status={}, count={}, pois节点类型={}", status, totalCount, poisNode.getNodeType());
      
      if (!poisNode.isArray()) {
        LOGGER.warn("高德API返回的pois不是数组，节点类型: {}", poisNode.getNodeType());
        result.put("success", true);
        Map<String, Object> data = new HashMap<>();
        data.put("places", places);
        data.put("total", totalCount);
        data.put("page", req.getPage() == null ? 1 : req.getPage());
        data.put("pageSize", req.getPageSize() == null ? 20 : req.getPageSize());
        data.put("totalPages", 0);
        result.put("data", data);
        return result;
      }
      for (JsonNode poi : poisNode) {
        Map<String, Object> place = new HashMap<>();
        place.put("id", poi.path("id").asText());
        place.put("name", poi.path("name").asText());
        place.put("address", poi.path("address").asText(""));
        String locationStr = poi.path("location").asText();
        if (!locationStr.isEmpty()) {
          String[] loc = locationStr.split(",");
          if (loc.length == 2) {
            try {
              Map<String, Object> locObj = new HashMap<>();
              locObj.put("longitude", Double.parseDouble(loc[0]));
              locObj.put("latitude", Double.parseDouble(loc[1]));
              place.put("location", locObj);
            } catch (NumberFormatException e) {
              LOGGER.warn("解析坐标失败: {}", locationStr);
            }
          }
        }
        place.put("distance", poi.path("distance").asInt(0));
        place.put("type", poi.path("type").asText(""));
        place.put("tel", poi.path("tel").asText(""));
        place.put("business_area", poi.path("business_area").asText(""));
        places.add(place);
      }
      
      LOGGER.info("高德API搜索完成，位置: ({}, {}), 关键词: {}, 类型: {}, 找到 {} 个结果", 
          req.getLongitude(), req.getLatitude(), req.getKeywords(), req.getTypes(), places.size());
      
      result.put("success", true);
      Map<String, Object> data = new HashMap<>();
      data.put("places", places);
      int total = root.path("count").asInt(places.size());
      if (total == 0 && !places.isEmpty()) {
        total = places.size();
      }
      data.put("total", total);
      data.put("page", req.getPage() == null ? 1 : req.getPage());
      data.put("pageSize", req.getPageSize() == null ? 20 : req.getPageSize());
      data.put("totalPages", (int)Math.ceil((double)total / (req.getPageSize() == null ? 20.0 : req.getPageSize())));
      result.put("data", data);
    } catch (Exception ex) {
      LOGGER.error("搜索周边场所失败", ex);
      result.put("success", false);
      result.put("message", "搜索失败: " + ex.getMessage());
    }
    return result;
  }

  @Override
  public List<PlaceCandidate> sortByRealDistance(PlaceDistanceSortRequest request) {
    String mode = normalizeTransport(request.getTransportMode());
    if (amapKey == null || amapKey.isBlank()) {
      return sortByStraightDistance(request.getPlaces());
    }

    List<PlaceCandidate> candidates = new ArrayList<>();
    for (PlaceCandidate place : request.getPlaces()) {
      PlaceCandidate updated = enrichDistance(request.getOrigin(), place, mode);
      candidates.add(updated);
    }

    candidates.sort(Comparator.comparingInt(this::sortDistanceValue));
    return candidates;
  }

  private PlaceCandidate enrichDistance(PlaceLocation origin, PlaceCandidate place, String mode) {
    try {
      DistanceResult result = callAmapDirection(origin, place.getLocation(), mode);
      if (result != null) {
        place.setRealDistance(result.distance);
        place.setRealDuration(result.duration);
        return place;
      }
    } catch (IOException | InterruptedException ex) {
      LOGGER.warn("Failed to call AMap for place {}", place.getId(), ex);
    }
    return place;
  }

  private DistanceResult callAmapDirection(PlaceLocation origin, PlaceLocation destination, String mode)
      throws IOException, InterruptedException {
    String baseUrl = "walking".equals(mode)
        ? "https://restapi.amap.com/v3/direction/walking"
        : "https://restapi.amap.com/v3/direction/driving";

    Map<String, String> query = new LinkedHashMap<>();
    query.put("key", amapKey);
    query.put("origin", origin.getLongitude() + "," + origin.getLatitude());
    query.put("destination", destination.getLongitude() + "," + destination.getLatitude());
    if ("driving".equals(mode)) {
      query.put("strategy", "0");
    }

    String url = baseUrl + "?" + buildQuery(query);
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      LOGGER.warn("AMap response status {}", response.statusCode());
      return null;
    }

    JsonNode root = objectMapper.readTree(response.body());
    if (!"1".equals(root.path("status").asText())) {
      LOGGER.warn("AMap direction failed: {}", root.path("info").asText());
      return null;
    }

    JsonNode pathNode = root.path("route").path("paths").isArray()
        && root.path("route").path("paths").size() > 0
        ? root.path("route").path("paths").get(0)
        : null;
    if (pathNode == null) {
      return null;
    }

    return new DistanceResult(pathNode.path("distance").asInt(), pathNode.path("duration").asInt());
  }

  private List<PlaceCandidate> sortByStraightDistance(List<PlaceCandidate> places) {
    List<PlaceCandidate> sorted = new ArrayList<>(places);
    sorted.sort(Comparator.comparingInt(this::sortDistanceValue));
    return sorted;
  }

  private int sortDistanceValue(PlaceCandidate place) {
    if (place.getRealDistance() != null) {
      return place.getRealDistance();
    }
    if (place.getDistance() != null) {
      return place.getDistance();
    }
    return Integer.MAX_VALUE;
  }

  private String normalizeTransport(String transportMode) {
    if (transportMode == null || transportMode.isBlank()) {
      return DEFAULT_TRANSPORT;
    }
    String mode = transportMode.trim().toLowerCase();
    return "driving".equals(mode) ? "driving" : "walking";
  }

  private String buildQuery(Map<String, String> query) {
    return query.entrySet().stream()
        .map(
            entry ->
                java.net.URLEncoder.encode(entry.getKey(), java.nio.charset.StandardCharsets.UTF_8)
                    + "="
                    + java.net.URLEncoder.encode(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8))
        .collect(java.util.stream.Collectors.joining("&"));
  }

  private String resolveAmapKey() {
    // 优先使用环境变量，如果没有则使用硬编码的key
    String key = System.getenv("AMAP_KEY");
    if (key != null && !key.isBlank()) {
      return key;
    }
    String backendKey = System.getenv("REDSEEKER_AMAP_KEY");
    if (backendKey != null && !backendKey.isBlank()) {
      return backendKey;
    }
    // 使用硬编码的key（与路线规划模块一致）
    return AMAP_KEY;
  }

  private static final class DistanceResult {
    private final int distance;
    private final int duration;

    private DistanceResult(int distance, int duration) {
      this.distance = distance;
      this.duration = duration;
    }
  }
}

