package com.redseeker.route;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoutePlanStageAttraction {
  private Long id;
  private String name;
  private int order;

  @JsonProperty("distance_from_previous")
  private int distanceFromPrevious;

  @JsonProperty("duration_from_previous")
  private int durationFromPrevious;

  public RoutePlanStageAttraction() {
  }

  public RoutePlanStageAttraction(
      Long id, String name, int order, int distanceFromPrevious, int durationFromPrevious) {
    this.id = id;
    this.name = name;
    this.order = order;
    this.distanceFromPrevious = distanceFromPrevious;
    this.durationFromPrevious = durationFromPrevious;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public int getDistanceFromPrevious() {
    return distanceFromPrevious;
  }

  public void setDistanceFromPrevious(int distanceFromPrevious) {
    this.distanceFromPrevious = distanceFromPrevious;
  }

  public int getDurationFromPrevious() {
    return durationFromPrevious;
  }

  public void setDurationFromPrevious(int durationFromPrevious) {
    this.durationFromPrevious = durationFromPrevious;
  }
}
