package com.redseeker.route;

import java.util.ArrayList;
import java.util.List;

public class RoutePath {
  private double distance;
  private double duration;
  private List<RouteStep> steps = new ArrayList<>();
  private String polyline;

  public double getDistance() {
    return distance;
  }

  public void setDistance(double distance) {
    this.distance = distance;
  }

  public double getDuration() {
    return duration;
  }

  public void setDuration(double duration) {
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
