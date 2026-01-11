package com.redseeker.route;

import java.util.List;

public class RouteResult {
  private double totalDistance;
  private double totalDuration;
  private List<RoutePath> legs;
  private String fullPolyline;

  public RouteResult(double totalDistance, double totalDuration, List<RoutePath> legs, String fullPolyline) {
    this.totalDistance = totalDistance;
    this.totalDuration = totalDuration;
    this.legs = legs;
    this.fullPolyline = fullPolyline;
  }

  public double getTotalDistance() {
    return totalDistance;
  }

  public double getTotalDuration() {
    return totalDuration;
  }

  public List<RoutePath> getLegs() {
    return legs;
  }

  public String getFullPolyline() {
    return fullPolyline;
  }
}
