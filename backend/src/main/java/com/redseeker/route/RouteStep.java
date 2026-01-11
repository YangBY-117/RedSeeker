package com.redseeker.route;

public class RouteStep {
  private String instruction;
  private String road;
  private double distance;
  private double duration;
  private String polyline;

  public RouteStep() {}

  public RouteStep(String instruction, String road, double distance, double duration, String polyline) {
    this.instruction = instruction;
    this.road = road;
    this.distance = distance;
    this.duration = duration;
    this.polyline = polyline;
  }

  public String getInstruction() {
    return instruction;
  }

  public void setInstruction(String instruction) {
    this.instruction = instruction;
  }

  public String getRoad() {
    return road;
  }

  public void setRoad(String road) {
    this.road = road;
  }

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

  public String getPolyline() {
    return polyline;
  }

  public void setPolyline(String polyline) {
    this.polyline = polyline;
  }
}
