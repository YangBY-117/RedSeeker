package com.redseeker.route;

public class OrderedAttraction {
  private Long id;
  private String name;
  private int order;
  private double distanceFromPrevious;
  private double durationFromPrevious;

  public OrderedAttraction(Long id, String name, int order, double distanceFromPrevious, double durationFromPrevious) {
    this.id = id;
    this.name = name;
    this.order = order;
    this.distanceFromPrevious = distanceFromPrevious;
    this.durationFromPrevious = durationFromPrevious;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getOrder() {
    return order;
  }

  public double getDistanceFromPrevious() {
    return distanceFromPrevious;
  }

  public double getDurationFromPrevious() {
    return durationFromPrevious;
  }
}
