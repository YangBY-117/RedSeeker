package com.redseeker.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteResult {
  private List<Point> orderedPoints = new ArrayList<>();
  private List<RouteSegment> segments = new ArrayList<>();
  private double totalDistanceMeters;
  private long totalDurationSeconds;

  public RouteResult() {
  }

  public RouteResult(List<Point> orderedPoints, List<RouteSegment> segments,
      double totalDistanceMeters, long totalDurationSeconds) {
    this.orderedPoints = new ArrayList<>(orderedPoints);
    this.segments = new ArrayList<>(segments);
    this.totalDistanceMeters = totalDistanceMeters;
    this.totalDurationSeconds = totalDurationSeconds;
  }

  public List<Point> getOrderedPoints() {
    return Collections.unmodifiableList(orderedPoints);
  }

  public void setOrderedPoints(List<Point> orderedPoints) {
    this.orderedPoints = new ArrayList<>(orderedPoints);
  }

  public List<RouteSegment> getSegments() {
    return Collections.unmodifiableList(segments);
  }

  public void setSegments(List<RouteSegment> segments) {
    this.segments = new ArrayList<>(segments);
  }

  public double getTotalDistanceMeters() {
    return totalDistanceMeters;
  }

  public void setTotalDistanceMeters(double totalDistanceMeters) {
    this.totalDistanceMeters = totalDistanceMeters;
  }

  public long getTotalDurationSeconds() {
    return totalDurationSeconds;
  }

  public void setTotalDurationSeconds(long totalDurationSeconds) {
    this.totalDurationSeconds = totalDurationSeconds;
  }
}
