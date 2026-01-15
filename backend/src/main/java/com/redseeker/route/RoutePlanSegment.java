package com.redseeker.route;

import java.util.ArrayList;
import java.util.List;

public class RoutePlanSegment {
  private Long fromAttractionId;
  private Long toAttractionId;
  private int distance;
  private int duration;
  private String polyline;
  private List<RouteStep> steps = new ArrayList<>();

  public Long getFromAttractionId() {
    return fromAttractionId;
  }

  public void setFromAttractionId(Long fromAttractionId) {
    this.fromAttractionId = fromAttractionId;
  }

  public Long getToAttractionId() {
    return toAttractionId;
  }

  public void setToAttractionId(Long toAttractionId) {
    this.toAttractionId = toAttractionId;
  }

  public int getDistance() {
    return distance;
  }

  public void setDistance(int distance) {
    this.distance = distance;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  public String getPolyline() {
    return polyline;
  }

  public void setPolyline(String polyline) {
    this.polyline = polyline;
  }

  public List<RouteStep> getSteps() {
    return steps;
  }

  public void setSteps(List<RouteStep> steps) {
    this.steps = steps;
  }
}
