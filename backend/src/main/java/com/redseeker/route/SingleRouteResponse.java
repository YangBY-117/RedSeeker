package com.redseeker.route;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SingleRouteResponse {
  private RouteInfo route;
  private AttractionSummary attraction;

  @JsonProperty("start_location")
  private RouteLocation startLocation;

  public RouteInfo getRoute() {
    return route;
  }

  public void setRoute(RouteInfo route) {
    this.route = route;
  }

  public AttractionSummary getAttraction() {
    return attraction;
  }

  public void setAttraction(AttractionSummary attraction) {
    this.attraction = attraction;
  }

  public RouteLocation getStartLocation() {
    return startLocation;
  }

  public void setStartLocation(RouteLocation startLocation) {
    this.startLocation = startLocation;
  }
}
