package com.redseeker.route;

import java.util.ArrayList;
import java.util.List;

public class RoutePlan {
  private int totalDistance;
  private int totalDuration;
  private List<RoutePlanStage> stages = new ArrayList<>();
  private List<RoutePlanSegment> segments = new ArrayList<>();
  private String fullPolyline;

  public int getTotalDistance() {
    return totalDistance;
  }

  public void setTotalDistance(int totalDistance) {
    this.totalDistance = totalDistance;
  }

  public int getTotalDuration() {
    return totalDuration;
  }

  public void setTotalDuration(int totalDuration) {
    this.totalDuration = totalDuration;
  }

  public List<RoutePlanStage> getStages() {
    return stages;
  }

  public void setStages(List<RoutePlanStage> stages) {
    this.stages = stages;
  }

  public List<RoutePlanSegment> getSegments() {
    return segments;
  }

  public void setSegments(List<RoutePlanSegment> segments) {
    this.segments = segments;
  }

  public String getFullPolyline() {
    return fullPolyline;
  }

  public void setFullPolyline(String fullPolyline) {
    this.fullPolyline = fullPolyline;
  }
}
