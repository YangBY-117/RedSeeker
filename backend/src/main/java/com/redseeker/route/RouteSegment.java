package com.redseeker.route;

import java.util.List;

public class RouteSegment {
  private Long fromAttractionId;
  private Long toAttractionId;
  private double distance;
  private double duration;
  private String polyline;
  private List<RouteStep> steps;

  public RouteSegment(Long fromAttractionId, Long toAttractionId, double distance, double duration, String polyline,
      List<RouteStep> steps) {
    this.fromAttractionId = fromAttractionId;
    this.toAttractionId = toAttractionId;
    this.distance = distance;
    this.duration = duration;
    this.polyline = polyline;
    this.steps = steps;
  }

  public Long getFromAttractionId() {
    return fromAttractionId;
  }

  public Long getToAttractionId() {
    return toAttractionId;
  }

  public double getDistance() {
    return distance;
  }

  public double getDuration() {
    return duration;
  }

  public String getPolyline() {
    return polyline;
  }

  public List<RouteStep> getSteps() {
    return steps;
  }
}
