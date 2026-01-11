package com.redseeker.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class RouteMultipleRequest {
  @NotEmpty
  private List<Long> attractionIds;
  @Valid
  @NotNull
  private LocationDto startLocation;
  @Valid
  private LocationDto endLocation;
  private String transportMode = "driving";
  private String strategy = "history_first";

  public List<Long> getAttractionIds() {
    return attractionIds;
  }

  public void setAttractionIds(List<Long> attractionIds) {
    this.attractionIds = attractionIds;
  }

  public LocationDto getStartLocation() {
    return startLocation;
  }

  public void setStartLocation(LocationDto startLocation) {
    this.startLocation = startLocation;
  }

  public LocationDto getEndLocation() {
    return endLocation;
  }

  public void setEndLocation(LocationDto endLocation) {
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
