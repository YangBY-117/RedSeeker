package com.redseeker.route;

import jakarta.validation.constraints.NotNull;

public class SingleRouteRequest {
  @NotNull
  private Long attractionId;

  @NotNull
  private RouteLocation startLocation;

  private String transportMode;

  public Long getAttractionId() {
    return attractionId;
  }

  public void setAttractionId(Long attractionId) {
    this.attractionId = attractionId;
  }

  public RouteLocation getStartLocation() {
    return startLocation;
  }

  public void setStartLocation(RouteLocation startLocation) {
    this.startLocation = startLocation;
  }

  public String getTransportMode() {
    return transportMode;
  }

  public void setTransportMode(String transportMode) {
    this.transportMode = transportMode;
  }
}
