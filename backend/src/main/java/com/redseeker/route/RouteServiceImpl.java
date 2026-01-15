package com.redseeker.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redseeker.common.ErrorCode;
import com.redseeker.common.ServiceException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RouteServiceImpl implements RouteService {
  private static final Logger LOGGER = LoggerFactory.getLogger(RouteServiceImpl.class);
  private static final String DEFAULT_TRANSPORT = "driving";
  private static final String DEFAULT_STRATEGY = "history_first";

  private final String databaseUrl;
  private final String amapKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public RouteServiceImpl() {
    this.databaseUrl = resolveDatabaseUrl();
    this.amapKey = resolveAmapKey();
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public SingleRouteResponse planSingleRoute(SingleRouteRequest request) {
    AttractionSummary attraction = loadAttraction(request.getAttractionId());
    if (attraction == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "attraction not found");
    }

    RouteLocation start = normalizeLocation(request.getStartLocation());
    String mode = normalizeTransport(request.getTransportMode());
    RouteInfo routeInfo = fetchRoute(start, attraction, mode);

    SingleRouteResponse response = new SingleRouteResponse();
    response.setRoute(routeInfo);
    response.setAttraction(attraction);
    response.setStartLocation(start);
    return response;
  }

  @Override
  public RoutePlanResult planMultipleRoute(MultipleRouteRequest request) {
    List<AttractionSummary> attractions = loadAttractions(request.getAttractionIds());
    if (attractions.isEmpty()) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "no attractions found");
    }

    RouteLocation start = normalizeLocation(request.getStartLocation());
    RouteLocation end = request.getEndLocation() != null ? normalizeLocation(request.getEndLocation()) : start;
    String mode = normalizeTransport(request.getTransportMode());
    String strategy = request.getStrategy() == null ? DEFAULT_STRATEGY : request.getStrategy().trim();

    List<AttractionSummary> ordered =
        "shortest".equalsIgnoreCase(strategy)
            ? sortByShortestWithMultiPoint(start, end, attractions, mode)
            : sortByHistoryThenDistance(start, attractions, mode);

    LOGGER.info("规划路线: 起点=({}, {}), 终点=({}, {}), 景点数={}", 
        start.getLongitude(), start.getLatitude(),
        end.getLongitude(), end.getLatitude(),
        ordered.size());

    // 使用多点路径规划
    RouteInfo routeInfo = fetchMultiPointRoute(start, end, ordered, mode);
    
    RoutePlanResult result = new RoutePlanResult();
    result.setTotal_distance(routeInfo.getDistance());
    result.setTotal_duration(routeInfo.getDuration());
    
    // 将polyline解码为坐标点数组
    List<List<Double>> path = decodePolyline(routeInfo.getPolyline());
    
    LOGGER.info("解码后的路径点数: {}", path.size());
    
    // 如果polyline为空或无效，构建基本路径（起点 -> 景点 -> 终点）
    if (path.isEmpty()) {
      LOGGER.info("polyline为空，构建基本路径");
      path = buildBasicPath(start, end, ordered);
      LOGGER.info("构建的基本路径点数: {}", path.size());
    }
    
    result.setPath(path);
    
    return result;
  }

  @Override
  public RouteLocation getCurrentLocation() {
    RouteLocation location = new RouteLocation();
    location.setLongitude(121.4737);
    location.setLatitude(31.2208);
    location.setAddress("上海市黄浦区");
    return location;
  }

  private RouteLocation normalizeLocation(RouteLocation location) {
    if (location == null) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "start_location is required");
    }
    if (location.getLongitude() == null || location.getLatitude() == null) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "longitude/latitude are required");
    }
    return location;
  }

  private String normalizeTransport(String transportMode) {
    if (transportMode == null || transportMode.isBlank()) {
      return DEFAULT_TRANSPORT;
    }
    String mode = transportMode.trim().toLowerCase();
    return switch (mode) {
      case "walking", "transit", "driving" -> mode;
      default -> DEFAULT_TRANSPORT;
    };
  }

  private RouteInfo fetchRoute(RouteLocation start, AttractionSummary destination, String mode) {
    if (amapKey == null || amapKey.isBlank()) {
      return buildFallbackRoute(start, destination);
    }
    try {
      RouteInfo info = callAmapDirection(start, destination, mode);
      if (info != null) {
        return info;
      }
    } catch (IOException | InterruptedException ex) {
      LOGGER.warn("Failed to call AMap direction API, falling back", ex);
    }
    return buildFallbackRoute(start, destination);
  }

  /**
   * 多点路径规划
   */
  private RouteInfo fetchMultiPointRoute(
      RouteLocation start, RouteLocation end, List<AttractionSummary> attractions, String mode) {
    if (amapKey == null || amapKey.isBlank() || attractions.isEmpty()) {
      return buildFallbackMultiPointRoute(start, end, attractions);
    }
    try {
      RouteInfo info = callAmapMultiPointDirection(start, end, attractions, mode);
      if (info != null) {
        return info;
      }
    } catch (IOException | InterruptedException ex) {
      LOGGER.warn("Failed to call AMap multi-point direction API, falling back", ex);
    }
    return buildFallbackMultiPointRoute(start, end, attractions);
  }

  /**
   * 调用高德地图多点路径规划API
   */
  private RouteInfo callAmapMultiPointDirection(
      RouteLocation origin, RouteLocation destination, List<AttractionSummary> waypoints, String mode)
      throws IOException, InterruptedException {
    String baseUrl = switch (mode) {
      case "walking" -> "https://restapi.amap.com/v3/direction/walking";
      case "transit" -> "https://restapi.amap.com/v3/direction/transit/integrated";
      default -> "https://restapi.amap.com/v3/direction/driving";
    };

    String originParam = origin.getLongitude() + "," + origin.getLatitude();
    String destinationParam = destination.getLongitude() + "," + destination.getLatitude();
    
    // 构建途经点参数
    StringBuilder waypointsParam = new StringBuilder();
    for (int i = 0; i < waypoints.size(); i++) {
      if (i > 0) {
        waypointsParam.append("|");
      }
      waypointsParam.append(waypoints.get(i).getLongitude())
          .append(",")
          .append(waypoints.get(i).getLatitude());
    }

    Map<String, String> query = new LinkedHashMap<>();
    query.put("key", amapKey);
    query.put("origin", originParam);
    query.put("destination", destinationParam);
    if (!waypointsParam.isEmpty()) {
      query.put("waypoints", waypointsParam.toString());
    }
    if ("driving".equals(mode)) {
      query.put("strategy", "0"); // 速度优先
    }

    String url = baseUrl + "?" + buildQuery(query);
    HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
    HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      LOGGER.warn("AMap multi-point response status {}", response.statusCode());
      return null;
    }

    JsonNode root = objectMapper.readTree(response.body());
    if (!"1".equals(root.path("status").asText())) {
      LOGGER.warn("AMap multi-point direction failed: {}", root.path("info").asText());
      return null;
    }

    if ("transit".equals(mode)) {
      return parseTransitRoute(root);
    }
    return parseStandardRoute(root);
  }

  /**
   * 构建多点路径的降级方案
   */
  private RouteInfo buildFallbackMultiPointRoute(
      RouteLocation start, RouteLocation end, List<AttractionSummary> attractions) {
    int totalDistance = 0;
    int totalDuration = 0;
    StringBuilder polylineBuilder = new StringBuilder();
    
    RouteLocation current = start;
    for (AttractionSummary attraction : attractions) {
      double distance = haversine(
          current.getLatitude(), current.getLongitude(),
          attraction.getLatitude(), attraction.getLongitude());
      totalDistance += (int) Math.round(distance);
      totalDuration += (int) Math.round(distance / 1.2);
      current = toLocation(attraction);
    }
    
    if (end != null && !attractions.isEmpty()) {
      double distance = haversine(
          current.getLatitude(), current.getLongitude(),
          end.getLatitude(), end.getLongitude());
      totalDistance += (int) Math.round(distance);
      totalDuration += (int) Math.round(distance / 1.2);
    }

    RouteInfo info = new RouteInfo();
    info.setDistance(totalDistance);
    info.setDuration(totalDuration);
    info.setPolyline(null);
    return info;
  }

  /**
   * 解码polyline为坐标点数组
   * 高德地图的polyline格式：经度,纬度;经度,纬度;...
   */
  private List<List<Double>> decodePolyline(String polyline) {
    List<List<Double>> path = new ArrayList<>();
    if (polyline == null || polyline.isBlank()) {
      return path;
    }
    
    // 高德地图polyline格式：经度,纬度;经度,纬度;...
    String[] points = polyline.split(";");
    for (String point : points) {
      if (point == null || point.isBlank()) {
        continue;
      }
      String[] coords = point.split(",");
      if (coords.length >= 2) {
        try {
          double lng = Double.parseDouble(coords[0].trim());
          double lat = Double.parseDouble(coords[1].trim());
          // 验证坐标是否有效
          if (!Double.isNaN(lng) && !Double.isNaN(lat) && 
              !Double.isInfinite(lng) && !Double.isInfinite(lat)) {
            path.add(List.of(lng, lat));
          }
        } catch (NumberFormatException e) {
          LOGGER.warn("Failed to parse polyline point: {}", point);
        }
      }
    }
    return path;
  }

  /**
   * 构建基本路径（起点 -> 景点 -> 终点）
   * 当polyline为空时使用
   */
  private List<List<Double>> buildBasicPath(
      RouteLocation start, RouteLocation end, List<AttractionSummary> attractions) {
    List<List<Double>> path = new ArrayList<>();
    
    // 添加起点
    if (start != null && start.getLongitude() != null && start.getLatitude() != null) {
      path.add(List.of(start.getLongitude(), start.getLatitude()));
      LOGGER.debug("添加起点: ({}, {})", start.getLongitude(), start.getLatitude());
    }
    
    // 添加景点
    int validAttractions = 0;
    for (AttractionSummary attraction : attractions) {
      if (attraction != null && attraction.getLongitude() != null && 
          attraction.getLatitude() != null) {
        path.add(List.of(attraction.getLongitude(), attraction.getLatitude()));
        validAttractions++;
        LOGGER.debug("添加景点 {}: ({}, {})", attraction.getName(), 
            attraction.getLongitude(), attraction.getLatitude());
      } else {
        LOGGER.warn("景点 {} 坐标无效: lng={}, lat={}", 
            attraction != null ? attraction.getName() : "null",
            attraction != null ? attraction.getLongitude() : "null",
            attraction != null ? attraction.getLatitude() : "null");
      }
    }
    LOGGER.info("有效景点数: {}/{}", validAttractions, attractions.size());
    
    // 添加终点（如果终点与起点不同）
    if (end != null && end.getLongitude() != null && end.getLatitude() != null) {
      // 检查终点是否与起点相同
      boolean isEndSameAsStart = start != null && 
          start.getLongitude() != null && start.getLatitude() != null &&
          Math.abs(start.getLongitude() - end.getLongitude()) < 0.0001 &&
          Math.abs(start.getLatitude() - end.getLatitude()) < 0.0001;
      
      if (!isEndSameAsStart) {
        path.add(List.of(end.getLongitude(), end.getLatitude()));
        LOGGER.debug("添加终点: ({}, {})", end.getLongitude(), end.getLatitude());
      } else {
        LOGGER.debug("终点与起点相同，跳过");
      }
    }
    
    LOGGER.info("构建的基本路径总点数: {}", path.size());
    return path;
  }

  private RouteInfo callAmapDirection(RouteLocation origin, AttractionSummary destination, String mode)
      throws IOException, InterruptedException {
    String baseUrl = switch (mode) {
      case "walking" -> "https://restapi.amap.com/v3/direction/walking";
      case "transit" -> "https://restapi.amap.com/v3/direction/transit/integrated";
      default -> "https://restapi.amap.com/v3/direction/driving";
    };

    String originParam = origin.getLongitude() + "," + origin.getLatitude();
    String destinationParam = destination.getLongitude() + "," + destination.getLatitude();
    Map<String, String> query = new LinkedHashMap<>();
    query.put("key", amapKey);
    query.put("origin", originParam);
    query.put("destination", destinationParam);
    if ("driving".equals(mode)) {
      query.put("strategy", "0");
    }

    String url = baseUrl + "?" + buildQuery(query);
    HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
    HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      LOGGER.warn("AMap response status {}", response.statusCode());
      return null;
    }

    JsonNode root = objectMapper.readTree(response.body());
    if (!"1".equals(root.path("status").asText())) {
      LOGGER.warn("AMap direction failed: {}", root.path("info").asText());
      return null;
    }

    if ("transit".equals(mode)) {
      return parseTransitRoute(root);
    }
    return parseStandardRoute(root);
  }

  private RouteInfo parseStandardRoute(JsonNode root) {
    JsonNode routeNode = root.path("route");
    JsonNode pathNode = routeNode.path("paths").isArray() && routeNode.path("paths").size() > 0
        ? routeNode.path("paths").get(0)
        : null;
    if (pathNode == null) {
      return null;
    }

    RouteInfo info = new RouteInfo();
    info.setDistance(pathNode.path("distance").asInt());
    info.setDuration(pathNode.path("duration").asInt());
    info.setPolyline(pathNode.path("polyline").asText(null));
    List<RouteStep> steps = new ArrayList<>();
    if (pathNode.path("steps").isArray()) {
      for (JsonNode step : pathNode.path("steps")) {
        steps.add(
            new RouteStep(
                step.path("instruction").asText(null),
                step.path("road").asText(null),
                step.path("distance").asInt(),
                step.path("duration").asInt(),
                step.path("polyline").asText(null)));
      }
    }
    info.setSteps(steps);
    return info;
  }

  private RouteInfo parseTransitRoute(JsonNode root) {
    JsonNode transit = root.path("route").path("transits");
    if (!transit.isArray() || transit.isEmpty()) {
      return null;
    }
    JsonNode first = transit.get(0);
    RouteInfo info = new RouteInfo();
    info.setDistance(first.path("distance").asInt());
    info.setDuration(first.path("duration").asInt());
    info.setPolyline(null);
    List<RouteStep> steps = new ArrayList<>();
    if (first.path("segments").isArray()) {
      for (JsonNode segment : first.path("segments")) {
        JsonNode walking = segment.path("walking");
        if (walking.has("steps")) {
          for (JsonNode step : walking.path("steps")) {
            steps.add(
                new RouteStep(
                    step.path("instruction").asText(null),
                    step.path("road").asText(null),
                    step.path("distance").asInt(),
                    step.path("duration").asInt(),
                    step.path("polyline").asText(null)));
          }
        }
      }
    }
    info.setSteps(steps);
    return info;
  }

  private String buildQuery(Map<String, String> query) {
    return query.entrySet().stream()
        .map(
            entry ->
                URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }

  private RouteInfo buildFallbackRoute(RouteLocation origin, AttractionSummary destination) {
    double distance = haversine(origin.getLatitude(), origin.getLongitude(), destination.getLatitude(),
        destination.getLongitude());
    RouteInfo info = new RouteInfo();
    info.setDistance((int) Math.round(distance));
    info.setDuration((int) Math.round(distance / 1.2));
    info.setPolyline(null);
    info.setSteps(
        List.of(
            new RouteStep(
                "从起点前往 " + destination.getName(),
                null,
                info.getDistance(),
                info.getDuration(),
                null)));
    return info;
  }

  private RoutePlan buildRoutePlan(
      RouteLocation start, RouteLocation end, List<AttractionSummary> ordered, String mode) {
    RoutePlan plan = new RoutePlan();
    List<RoutePlanSegment> segments = new ArrayList<>();
    List<RoutePlanStage> stages = buildStages(ordered);
    plan.setStages(stages);

    int totalDistance = 0;
    int totalDuration = 0;
    StringBuilder polyline = new StringBuilder();

    RouteLocation current = start;
    Long currentId = null;
    for (AttractionSummary attraction : ordered) {
      RouteInfo info = fetchRoute(current, attraction, mode);
      RoutePlanSegment segment = new RoutePlanSegment();
      segment.setFromAttractionId(currentId);
      segment.setToAttractionId(attraction.getId());
      segment.setDistance(info.getDistance());
      segment.setDuration(info.getDuration());
      segment.setPolyline(info.getPolyline());
      segment.setSteps(info.getSteps());
      segments.add(segment);
      totalDistance += info.getDistance();
      totalDuration += info.getDuration();
      appendPolyline(polyline, info.getPolyline());

      current = toLocation(attraction);
      currentId = attraction.getId();
    }

    if (end != null && ordered.size() > 0) {
      RouteInfo info = fetchRoute(current, new AttractionSummary(null, "终点", end.getAddress(),
          end.getLongitude(), end.getLatitude()), mode);
      RoutePlanSegment segment = new RoutePlanSegment();
      segment.setFromAttractionId(currentId);
      segment.setToAttractionId(null);
      segment.setDistance(info.getDistance());
      segment.setDuration(info.getDuration());
      segment.setPolyline(info.getPolyline());
      segment.setSteps(info.getSteps());
      segments.add(segment);
      totalDistance += info.getDistance();
      totalDuration += info.getDuration();
      appendPolyline(polyline, info.getPolyline());
    }

    plan.setSegments(segments);
    plan.setTotalDistance(totalDistance);
    plan.setTotalDuration(totalDuration);
    plan.setFullPolyline(polyline.length() > 0 ? polyline.toString() : null);
    return plan;
  }

  private List<RoutePlanStage> buildStages(List<AttractionSummary> ordered) {
    Map<String, RoutePlanStage> stageMap = new LinkedHashMap<>();
    for (AttractionSummary attraction : ordered) {
      String stageKey = stageKey(attraction);
      RoutePlanStage stage = stageMap.computeIfAbsent(stageKey, key -> {
        RoutePlanStage newStage = new RoutePlanStage();
        newStage.setStageName(attraction.getStageName() != null ? attraction.getStageName() : "历史阶段");
        if (attraction.getStageStart() != null) {
          String period = attraction.getStageStart() + "-" +
              (attraction.getStageEnd() != null ? attraction.getStageEnd() : "");
          newStage.setStagePeriod(period);
        }
        return newStage;
      });
      int order = stage.getAttractions().size() + 1;
      stage.getAttractions().add(new RoutePlanStageAttraction(attraction.getId(), attraction.getName(), order, 0, 0));
    }
    return new ArrayList<>(stageMap.values());
  }

  private String stageKey(AttractionSummary attraction) {
    if (attraction.getStageStart() == null) {
      return attraction.getStageName() != null ? attraction.getStageName() : "unknown";
    }
    return attraction.getStageStart() + ":" + attraction.getStageEnd();
  }

  private void appendPolyline(StringBuilder builder, String polyline) {
    if (polyline == null || polyline.isBlank()) {
      return;
    }
    if (builder.length() > 0) {
      builder.append(";");
    }
    builder.append(polyline);
  }

  private RouteLocation toLocation(AttractionSummary attraction) {
    RouteLocation location = new RouteLocation();
    location.setLongitude(attraction.getLongitude());
    location.setLatitude(attraction.getLatitude());
    location.setAddress(attraction.getAddress());
    return location;
  }

  private List<AttractionSummary> sortByShortest(
      RouteLocation start, List<AttractionSummary> attractions, String mode) {
    List<AttractionSummary> remaining = new ArrayList<>(attractions);
    List<AttractionSummary> ordered = new ArrayList<>();
    RouteLocation current = start;
    while (!remaining.isEmpty()) {
      AttractionSummary nearest = null;
      int nearestDistance = Integer.MAX_VALUE;
      for (AttractionSummary candidate : remaining) {
        int distance = estimateDistance(current, candidate, mode);
        if (distance < nearestDistance) {
          nearestDistance = distance;
          nearest = candidate;
        }
      }
      if (nearest == null) {
        break;
      }
      ordered.add(nearest);
      remaining.remove(nearest);
      current = toLocation(nearest);
    }
    return ordered;
  }

  /**
   * 最短路径策略（使用多点路径规划）
   * 注意：高德API会自动优化途经点顺序，所以这里只需要按原始顺序传入即可
   */
  private List<AttractionSummary> sortByShortestWithMultiPoint(
      RouteLocation start, RouteLocation end, List<AttractionSummary> attractions, String mode) {
    // 对于最短路径，高德API会自动优化顺序，所以保持原始顺序即可
    // 或者使用简单的贪心算法预排序
    return sortByShortest(start, attractions, mode);
  }

  /**
   * 历史优先策略：按历史事件的start_year排序
   * 先按历史阶段（start_year）排序，同阶段内按最短路径排序
   */
  private List<AttractionSummary> sortByHistoryThenDistance(
      RouteLocation start, List<AttractionSummary> attractions, String mode) {
    // 按历史事件的start_year排序
    List<AttractionSummary> sorted = new ArrayList<>(attractions);
    sorted.sort((a, b) -> {
      Integer aStart = a.getStageStart();
      Integer bStart = b.getStageStart();
      if (aStart == null && bStart == null) {
        return 0;
      }
      if (aStart == null) {
        return 1; // 没有历史事件的排在后面
      }
      if (bStart == null) {
        return -1;
      }
      int yearCompare = aStart.compareTo(bStart);
      if (yearCompare != 0) {
        return yearCompare;
      }
      // 同一年份，按end_year排序
      Integer aEnd = a.getStageEnd();
      Integer bEnd = b.getStageEnd();
      if (aEnd == null && bEnd == null) {
        return 0;
      }
      if (aEnd == null) {
        return 1;
      }
      if (bEnd == null) {
        return -1;
      }
      return aEnd.compareTo(bEnd);
    });

    // 同历史阶段内的景点，按最短路径排序
    Map<String, List<AttractionSummary>> grouped = sorted.stream()
        .collect(Collectors.groupingBy(this::stageKey, LinkedHashMap::new, Collectors.toList()));

    List<AttractionSummary> ordered = new ArrayList<>();
    RouteLocation current = start;
    List<Map.Entry<String, List<AttractionSummary>>> entries = new ArrayList<>(grouped.entrySet());
    entries.sort(Comparator.comparingInt(entry -> minStageYear(entry.getValue())));
    for (Map.Entry<String, List<AttractionSummary>> entry : entries) {
      List<AttractionSummary> stageAttractions = entry.getValue();
      List<AttractionSummary> stageOrdered = sortByShortest(current, stageAttractions, mode);
      if (!stageOrdered.isEmpty()) {
        ordered.addAll(stageOrdered);
        current = toLocation(stageOrdered.get(stageOrdered.size() - 1));
      }
    }
    return ordered;
  }

  private int minStageYear(List<AttractionSummary> attractions) {
    return attractions.stream()
        .map(AttractionSummary::getStageStart)
        .filter(Objects::nonNull)
        .min(Integer::compareTo)
        .orElse(Integer.MAX_VALUE);
  }

  private int estimateDistance(RouteLocation origin, AttractionSummary destination, String mode) {
    double distance = haversine(origin.getLatitude(), origin.getLongitude(), destination.getLatitude(),
        destination.getLongitude());
    return (int) Math.round(distance);
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

  private AttractionSummary loadAttraction(Long attractionId) {
    String sql =
        "SELECT id, name, address, longitude, latitude, he.start_year, he.end_year, he.period "
            + "FROM attractions a "
            + "LEFT JOIN attraction_events ae ON ae.attraction_id = a.id "
            + "LEFT JOIN historical_events he ON he.id = ae.event_id "
            + "WHERE a.id = ? LIMIT 1";
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, attractionId);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          AttractionSummary summary =
              new AttractionSummary(
                  rs.getLong("id"),
                  rs.getString("name"),
                  rs.getString("address"),
                  rs.getDouble("longitude"),
                  rs.getDouble("latitude"));
          summary.setStageName(rs.getString("period"));
          Integer start = rs.getObject("start_year", Integer.class);
          Integer end = rs.getObject("end_year", Integer.class);
          summary.setStageStart(start);
          summary.setStageEnd(end);
          return summary;
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to load attraction {}", attractionId, ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to load attraction");
    }
    return null;
  }

  private List<AttractionSummary> loadAttractions(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Collections.emptyList();
    }
    String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(", "));
    String sql =
        "SELECT a.id, a.name, a.address, a.longitude, a.latitude, he.start_year, he.end_year, he.period "
            + "FROM attractions a "
            + "LEFT JOIN attraction_events ae ON ae.attraction_id = a.id "
            + "LEFT JOIN historical_events he ON he.id = ae.event_id "
            + "WHERE a.id IN (" + placeholders + ")";
    Map<Long, AttractionSummary> summaries = new LinkedHashMap<>();
    try (Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < ids.size(); i++) {
        statement.setLong(i + 1, ids.get(i));
      }
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          long id = rs.getLong("id");
          String name = rs.getString("name");
          String address = rs.getString("address");
          double longitude = rs.getDouble("longitude");
          double latitude = rs.getDouble("latitude");
          AttractionSummary summary = summaries.get(id);
          if (summary == null) {
            summary = new AttractionSummary(id, name, address, longitude, latitude);
            summaries.put(id, summary);
          }
          Integer start = rs.getObject("start_year", Integer.class);
          if (start != null && (summary.getStageStart() == null || start < summary.getStageStart())) {
            summary.setStageStart(start);
            summary.setStageEnd(rs.getObject("end_year", Integer.class));
            summary.setStageName(rs.getString("period"));
          }
        }
      }
    } catch (SQLException ex) {
      LOGGER.error("Failed to load attractions", ex);
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to load attractions");
    }
    return ids.stream()
        .map(summaries::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
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

  private String resolveAmapKey() {
    // 优先使用环境变量
    String key = System.getenv("AMAP_KEY");
    if (key != null && !key.isBlank()) {
      return key;
    }
    String backendKey = System.getenv("REDSEEKER_AMAP_KEY");
    if (backendKey != null && !backendKey.isBlank()) {
      return backendKey;
    }
    // 默认使用新的API key
    return "2039f165180b1ece6c8cfb1ae448339b";
  }
}
