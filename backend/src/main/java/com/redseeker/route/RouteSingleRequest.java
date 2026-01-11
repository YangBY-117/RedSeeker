package com.redseeker.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class RouteSingleRequest {
  @NotNull
  private Long attractionId;
  @Valid
  @NotNull
  private LocationDto startLocation;
  private String transportMode = "driving";

  public Long getAttractionId() {
    return attractionId;
  }

  public void setAttractionId(Long attractionId) {
    this.attractionId = attractionId;
  }

  public LocationDto getStartLocation() {
    return startLocation;
  }

  public void setStartLocation(LocationDto startLocation) {
    this.startLocation = startLocation;
  }

  public String getTransportMode() {
    return transportMode;
  }

  public void setTransportMode(String transportMode) {
    this.transportMode = transportMode;
  }
}
