package com.redseeker.route;

import com.redseeker.common.ErrorCode;
import com.redseeker.common.ServiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AmapClient {
  private final RestTemplate restTemplate = new RestTemplate();
  private final AmapProperties properties;

  public AmapClient(AmapProperties properties) {
    this.properties = properties;
  }

  public RoutePath planRoute(LocationDto origin, LocationDto destination, String mode) {
    String endpoint = switch (mode) {
      case "walking" -> "/v3/direction/walking";
      case "transit" -> "/v3/direction/transit/integrated";
      default -> "/v3/direction/driving";
    };

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("key", properties.getKey());
    params.add("origin", origin.getLongitude() + "," + origin.getLatitude());
    params.add("destination", destination.getLongitude() + "," + destination.getLatitude());
    if ("transit".equals(mode)) {
      params.add("strategy", "0");
      params.add("city", "010");
    }

    String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + endpoint)
        .queryParams(params)
        .toUriString();

    Map<String, Object> response = restTemplate.getForObject(url, Map.class);
    if (response == null || !Objects.equals("1", response.get("status"))) {
      throw new ServiceException(ErrorCode.INTERNAL_ERROR, "高德路线规划失败");
    }

    Map<String, Object> route = (Map<String, Object>) response.get("route");
    if (route == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "高德未返回路线数据");
    }

    if (route.containsKey("paths")) {
      return parsePathRoute(route);
    }
    return parseTransitRoute(route);
  }

  private RoutePath parsePathRoute(Map<String, Object> route) {
    List<Map<String, Object>> paths = (List<Map<String, Object>>) route.get("paths");
    if (paths == null || paths.isEmpty()) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "高德未返回路径信息");
    }
    Map<String, Object> path = paths.get(0);
    RoutePath routePath = new RoutePath();
    routePath.setDistance(parseDouble(path.get("distance")));
    routePath.setDuration(parseDouble(path.get("duration")));
    routePath.setPolyline((String) path.getOrDefault("polyline", ""));

    List<RouteStep> steps = new ArrayList<>();
    List<Map<String, Object>> rawSteps = (List<Map<String, Object>>) path.get("steps");
    if (rawSteps != null) {
      for (Map<String, Object> step : rawSteps) {
        steps.add(new RouteStep(
            (String) step.getOrDefault("instruction", ""),
            (String) step.getOrDefault("road", ""),
            parseDouble(step.get("distance")),
            parseDouble(step.get("duration")),
            (String) step.getOrDefault("polyline", "")
        ));
      }
    }
    routePath.setSteps(steps);
    return routePath;
  }

  private RoutePath parseTransitRoute(Map<String, Object> route) {
    List<Map<String, Object>> transits = (List<Map<String, Object>>) route.get("transits");
    if (transits == null || transits.isEmpty()) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "高德未返回公交线路");
    }
    Map<String, Object> transit = transits.get(0);
    RoutePath routePath = new RoutePath();
    routePath.setDistance(parseDouble(transit.get("distance")));
    routePath.setDuration(parseDouble(transit.get("duration")));

    List<RouteStep> steps = new ArrayList<>();
    List<Map<String, Object>> segments = (List<Map<String, Object>>) transit.get("segments");
    if (segments != null) {
      for (Map<String, Object> segment : segments) {
        Map<String, Object> walking = (Map<String, Object>) segment.get("walking");
        if (walking != null) {
          List<Map<String, Object>> walkingSteps = (List<Map<String, Object>>) walking.get("steps");
          if (walkingSteps != null) {
            for (Map<String, Object> step : walkingSteps) {
              steps.add(new RouteStep(
                  (String) step.getOrDefault("instruction", ""),
                  (String) step.getOrDefault("road", ""),
                  parseDouble(step.get("distance")),
                  parseDouble(step.get("duration")),
                  (String) step.getOrDefault("polyline", "")
              ));
            }
          }
        }
        Map<String, Object> bus = (Map<String, Object>) segment.get("bus");
        if (bus != null) {
          steps.add(new RouteStep("乘坐公交", "公交线路", parseDouble(bus.get("distance")),
              parseDouble(bus.get("duration")), ""));
        }
      }
    }
    routePath.setSteps(steps);
    routePath.setPolyline("transit");
    return routePath;
  }

  private double parseDouble(Object value) {
    if (value == null) {
      return 0;
    }
    try {
      return Double.parseDouble(value.toString());
    } catch (NumberFormatException ex) {
      return 0;
    }
  }
}
