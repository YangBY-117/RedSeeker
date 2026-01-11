package com.redseeker.route;

import java.util.List;

public class RoutePlan {
  private double totalDistance;
  private double totalDuration;
  private String transportMode;
  private List<String> storyPoints;
  private List<RouteStage> stages;
  private List<RouteSegment> segments;
  private String fullPolyline;

  public RoutePlan(double totalDistance, double totalDuration, String transportMode, List<String> storyPoints,
      List<RouteStage> stages, List<RouteSegment> segments, String fullPolyline) {
    this.totalDistance = totalDistance;
    this.totalDuration = totalDuration;
    this.transportMode = transportMode;
    this.storyPoints = storyPoints;
    this.stages = stages;
    this.segments = segments;
    this.fullPolyline = fullPolyline;
  }

  public double getTotalDistance() {
    return totalDistance;
  }

  public double getTotalDuration() {
    return totalDuration;
  }

  public String getTransportMode() {
    return transportMode;
  }

  public List<String> getStoryPoints() {
    return storyPoints;
  }

  public List<RouteStage> getStages() {
    return stages;
  }

  public List<RouteSegment> getSegments() {
    return segments;
  }

  public String getFullPolyline() {
    return fullPolyline;
  }
}
