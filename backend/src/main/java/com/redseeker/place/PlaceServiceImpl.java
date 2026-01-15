package com.redseeker.place;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlaceServiceImpl implements PlaceService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PlaceServiceImpl.class);
  private static final String DEFAULT_TRANSPORT = "walking";

  private final String amapKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public PlaceServiceImpl() {
    this.amapKey = resolveAmapKey();
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
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
                URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }

  private String resolveAmapKey() {
    String key = System.getenv("AMAP_KEY");
    if (key != null && !key.isBlank()) {
      return key;
    }
    String backendKey = System.getenv("REDSEEKER_AMAP_KEY");
    return backendKey != null && !backendKey.isBlank() ? backendKey : null;
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
