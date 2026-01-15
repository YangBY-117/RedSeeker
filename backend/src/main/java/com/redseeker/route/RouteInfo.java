package com.redseeker.route;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class RouteInfo {
  private int distance;
  private int duration;
  private List<RouteStep> steps = new ArrayList<>();

  @JsonProperty("polyline")
  private String polyline;

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

  public List<RouteStep> getSteps() {
    return steps;
  }

  public void setSteps(List<RouteStep> steps) {
    this.steps = steps;
  }

  public String getPolyline() {
    return polyline;
  }

  public void setPolyline(String polyline) {
    this.polyline = polyline;
  }
}
