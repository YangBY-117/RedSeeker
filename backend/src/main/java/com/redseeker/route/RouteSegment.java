package com.redseeker.route;

public class RouteSegment {
  private Point from;
  private Point to;
  private double distanceMeters;
  private long durationSeconds;

  public RouteSegment() {
  }

  public RouteSegment(Point from, Point to, double distanceMeters, long durationSeconds) {
    this.from = from;
    this.to = to;
    this.distanceMeters = distanceMeters;
    this.durationSeconds = durationSeconds;
  }

  public Point getFrom() {
    return from;
  }

  public void setFrom(Point from) {
    this.from = from;
  }

  public Point getTo() {
    return to;
  }

  public void setTo(Point to) {
    this.to = to;
  }

  public double getDistanceMeters() {
    return distanceMeters;
  }

  public void setDistanceMeters(double distanceMeters) {
    this.distanceMeters = distanceMeters;
  }

  public long getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds(long durationSeconds) {
    this.durationSeconds = durationSeconds;
  }
}
