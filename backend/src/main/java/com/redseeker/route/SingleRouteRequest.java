package com.redseeker.route;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class SingleRouteRequest {
  @NotNull
  @JsonProperty("attraction_id")
  private Long attractionId;

  @NotNull
  @JsonProperty("start_location")
  private RouteLocation startLocation;

  @JsonProperty("transport_mode")
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
