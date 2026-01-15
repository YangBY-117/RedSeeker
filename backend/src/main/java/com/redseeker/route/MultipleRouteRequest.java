package com.redseeker.route;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class MultipleRouteRequest {
  @NotEmpty
  private List<Long> attractionIds;

  @NotNull
  private RouteLocation startLocation;

  private RouteLocation endLocation;

  private String transportMode;

  private String strategy;

  public List<Long> getAttractionIds() {
    return attractionIds;
  }

  public void setAttractionIds(List<Long> attractionIds) {
    this.attractionIds = attractionIds;
  }

  public RouteLocation getStartLocation() {
    return startLocation;
  }

  public void setStartLocation(RouteLocation startLocation) {
    this.startLocation = startLocation;
  }

  public RouteLocation getEndLocation() {
    return endLocation;
  }

  public void setEndLocation(RouteLocation endLocation) {
    this.endLocation = endLocation;
  }

  public String getTransportMode() {
    return transportMode;
  }

  public void setTransportMode(String transportMode) {
    this.transportMode = transportMode;
  }

  public String getStrategy() {
    return strategy;
  }

  public void setStrategy(String strategy) {
    this.strategy = strategy;
  }
}
