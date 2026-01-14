package com.redseeker.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RouteServiceImpl implements RouteService {
  private static final double EARTH_RADIUS_METERS = 6371000.0;
  private static final double DEFAULT_SPEED_KMH = 30.0;
  private static final double SECONDS_PER_HOUR = 3600.0;

  @Override
  public RouteResult calculateRoute(List<Point> points) {
    if (points == null) {
      throw new IllegalArgumentException("points must not be null");
    }
    if (points.size() <= 1) {
      return new RouteResult(new ArrayList<>(points), Collections.emptyList(), 0.0, 0);
    }

    List<Point> ordered = buildNearestNeighborRoute(points);
    ordered = improveWithTwoOpt(ordered);
    return buildResult(ordered);
  }

  private List<Point> buildNearestNeighborRoute(List<Point> points) {
    List<Point> remaining = new ArrayList<>(points);
    List<Point> route = new ArrayList<>();
    Point current = remaining.remove(0);
    route.add(current);
    while (!remaining.isEmpty()) {
      Point next = null;
      double bestDistance = Double.MAX_VALUE;
      for (Point candidate : remaining) {
        double distance = haversineDistanceMeters(current, candidate);
        if (distance < bestDistance) {
          bestDistance = distance;
          next = candidate;
        }
      }
      current = next;
      route.add(current);
      remaining.remove(current);
    }
    return route;
  }

  private List<Point> improveWithTwoOpt(List<Point> route) {
    List<Point> optimized = new ArrayList<>(route);
    boolean improved = true;
    while (improved) {
      improved = false;
      for (int i = 1; i < optimized.size() - 2; i++) {
        for (int k = i + 1; k < optimized.size() - 1; k++) {
          double delta = calculateSwapDelta(optimized, i, k);
          if (delta < -0.0001) {
            reverseSegment(optimized, i, k);
            improved = true;
          }
        }
      }
    }
    return optimized;
  }

  private double calculateSwapDelta(List<Point> route, int i, int k) {
    Point a = route.get(i - 1);
    Point b = route.get(i);
    Point c = route.get(k);
    Point d = route.get(k + 1);
    double current = haversineDistanceMeters(a, b) + haversineDistanceMeters(c, d);
    double swapped = haversineDistanceMeters(a, c) + haversineDistanceMeters(b, d);
    return swapped - current;
  }

  private void reverseSegment(List<Point> route, int i, int k) {
    while (i < k) {
      Point temp = route.get(i);
      route.set(i, route.get(k));
      route.set(k, temp);
      i++;
      k--;
    }
  }

  private RouteResult buildResult(List<Point> ordered) {
    List<RouteSegment> segments = new ArrayList<>();
    double totalDistance = 0.0;
    long totalDuration = 0;
    for (int i = 0; i < ordered.size() - 1; i++) {
      Point from = ordered.get(i);
      Point to = ordered.get(i + 1);
      double distance = haversineDistanceMeters(from, to);
      long duration = estimateDurationSeconds(distance);
      segments.add(new RouteSegment(from, to, distance, duration));
      totalDistance += distance;
      totalDuration += duration;
    }
    return new RouteResult(ordered, segments, totalDistance, totalDuration);
  }

  private long estimateDurationSeconds(double distanceMeters) {
    double speedMetersPerSecond = (DEFAULT_SPEED_KMH * 1000.0) / SECONDS_PER_HOUR;
    return Math.round(distanceMeters / speedMetersPerSecond);
  }

  private double haversineDistanceMeters(Point a, Point b) {
    double lat1 = Math.toRadians(a.getLatitude());
    double lat2 = Math.toRadians(b.getLatitude());
    double deltaLat = lat2 - lat1;
    double deltaLon = Math.toRadians(b.getLongitude() - a.getLongitude());
    double sinLat = Math.sin(deltaLat / 2.0);
    double sinLon = Math.sin(deltaLon / 2.0);
    double h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
    double c = 2.0 * Math.atan2(Math.sqrt(h), Math.sqrt(1.0 - h));
    return EARTH_RADIUS_METERS * c;
  }
}
