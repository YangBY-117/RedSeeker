package com.redseeker.route;

import java.util.List;

public class RouteMultipleResponse {
  private RoutePlan routePlan;
  private List<AttractionInfo> attractions;

  public RouteMultipleResponse(RoutePlan routePlan, List<AttractionInfo> attractions) {
    this.routePlan = routePlan;
    this.attractions = attractions;
  }

  public RoutePlan getRoutePlan() {
    return routePlan;
  }

  public List<AttractionInfo> getAttractions() {
    return attractions;
  }
}
