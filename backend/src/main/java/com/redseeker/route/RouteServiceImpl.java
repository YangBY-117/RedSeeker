package com.redseeker.route;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RouteServiceImpl implements RouteService {
  private final AmapClient amapClient;
  private final RouteRepository routeRepository;

  public RouteServiceImpl(AmapClient amapClient, RouteRepository routeRepository) {
    this.amapClient = amapClient;
    this.routeRepository = routeRepository;
  }

  @Override
  public RouteSingleResponse planSingle(RouteSingleRequest request) {
    AttractionInfo attraction = getAttraction(request.getAttractionId());
    LocationDto destination = new LocationDto(attraction.getLongitude(), attraction.getLatitude(), attraction.getAddress());
    RoutePath route = amapClient.planRoute(request.getStartLocation(), destination, request.getTransportMode());
    return new RouteSingleResponse(route, attraction, request.getStartLocation());
  }

  @Override
  public RouteMultipleResponse planMultiple(RouteMultipleRequest request) {
    List<AttractionInfo> attractions = request.getAttractionIds().stream()
        .map(routeRepository::getAttraction)
        .toList();

    List<AttractionInfo> ordered = orderAttractions(attractions, request.getStrategy());
    List<RouteSegment> segments = new ArrayList<>();
    List<String> polylines = new ArrayList<>();

    LocationDto current = request.getStartLocation();
    Long previousAttractionId = null;
    double totalDistance = 0;
    double totalDuration = 0;

    for (AttractionInfo attraction : ordered) {
      LocationDto destination = new LocationDto(attraction.getLongitude(), attraction.getLatitude(), attraction.getAddress());
      RoutePath routePath = amapClient.planRoute(current, destination, request.getTransportMode());
      segments.add(new RouteSegment(previousAttractionId, attraction.getId(), routePath.getDistance(),
          routePath.getDuration(), routePath.getPolyline(), routePath.getSteps()));
      totalDistance += routePath.getDistance();
      totalDuration += routePath.getDuration();
      if (routePath.getPolyline() != null && !routePath.getPolyline().isBlank()) {
        polylines.add(routePath.getPolyline());
      }
      current = destination;
      previousAttractionId = attraction.getId();
    }

    LocationDto end = request.getEndLocation() != null ? request.getEndLocation() : request.getStartLocation();
    if (request.getEndLocation() != null) {
      RoutePath routePath = amapClient.planRoute(current, end, request.getTransportMode());
      segments.add(new RouteSegment(previousAttractionId, null, routePath.getDistance(), routePath.getDuration(),
          routePath.getPolyline(), routePath.getSteps()));
      totalDistance += routePath.getDistance();
      totalDuration += routePath.getDuration();
      if (routePath.getPolyline() != null && !routePath.getPolyline().isBlank()) {
        polylines.add(routePath.getPolyline());
      }
    }

    List<RouteStage> stages = buildStages(ordered);
    List<String> storyPoints = ordered.stream()
        .flatMap(attraction -> attraction.getStoryPoints().stream())
        .distinct()
        .toList();

    RoutePlan plan = new RoutePlan(totalDistance, totalDuration, request.getTransportMode(), storyPoints,
        stages, segments, String.join(";", polylines));
    return new RouteMultipleResponse(plan, attractions);
  }

  @Override
  public RouteResult calculateRoute(List<Point> points) {
    if (points == null || points.size() < 2) {
      return new RouteResult(0, 0, List.of(), "");
    }
    List<RoutePath> legs = new ArrayList<>();
    List<String> polylines = new ArrayList<>();
    double totalDistance = 0;
    double totalDuration = 0;

    for (int i = 0; i < points.size() - 1; i++) {
      Point start = points.get(i);
      Point end = points.get(i + 1);
      LocationDto origin = new LocationDto(start.getLongitude(), start.getLatitude(), null);
      LocationDto destination = new LocationDto(end.getLongitude(), end.getLatitude(), null);
      RoutePath routePath = amapClient.planRoute(origin, destination, "driving");
      legs.add(routePath);
      totalDistance += routePath.getDistance();
      totalDuration += routePath.getDuration();
      if (routePath.getPolyline() != null && !routePath.getPolyline().isBlank()) {
        polylines.add(routePath.getPolyline());
      }
    }

    return new RouteResult(totalDistance, totalDuration, legs, String.join(";", polylines));
  }

  @Override
  public LocationDto currentLocation() {
    return new LocationDto(121.4737, 31.2304, "上海市黄浦区人民广场");
  }

  private List<AttractionInfo> orderAttractions(List<AttractionInfo> attractions, String strategy) {
    if (Objects.equals("shortest", strategy)) {
      return new ArrayList<>(attractions);
    }

    return attractions.stream()
        .sorted(Comparator.comparingInt(AttractionInfo::getStartYear))
        .toList();
  }

  private List<RouteStage> buildStages(List<AttractionInfo> ordered) {
    Map<String, List<AttractionInfo>> grouped = ordered.stream()
        .collect(Collectors.groupingBy(AttractionInfo::getPeriod, LinkedHashMap::new, Collectors.toList()));

    List<RouteStage> stages = new ArrayList<>();
    for (Map.Entry<String, List<AttractionInfo>> entry : grouped.entrySet()) {
      List<OrderedAttraction> stageAttractions = new ArrayList<>();
      int order = 1;
      for (AttractionInfo attraction : entry.getValue()) {
        stageAttractions.add(new OrderedAttraction(attraction.getId(), attraction.getName(), order, 0, 0));
        order += 1;
      }
      stages.add(new RouteStage(entry.getKey(), entry.getKey(), stageAttractions));
    }
    return stages;
  }

  private AttractionInfo getAttraction(Long id) {
    return routeRepository.getAttraction(id);
  }
}
