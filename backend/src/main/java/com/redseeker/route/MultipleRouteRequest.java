package com.redseeker.route;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class MultipleRouteRequest {
  @NotEmpty
  @JsonProperty("attraction_ids")
  private List<Long> attractionIds;

  @NotNull
  @JsonProperty("start_location")
  private RouteLocation startLocation;

  @JsonProperty("end_location")
  private RouteLocation endLocation;

  @JsonProperty("transport_mode")
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
