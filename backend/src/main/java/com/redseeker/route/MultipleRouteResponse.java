package com.redseeker.route;

import java.util.ArrayList;
import java.util.List;

public class MultipleRouteResponse {
  private RoutePlan routePlan;
  private List<AttractionSummary> attractions = new ArrayList<>();

  public RoutePlan getRoutePlan() {
    return routePlan;
  }

  public void setRoutePlan(RoutePlan routePlan) {
    this.routePlan = routePlan;
  }

  public List<AttractionSummary> getAttractions() {
    return attractions;
  }

  public void setAttractions(List<AttractionSummary> attractions) {
    this.attractions = attractions;
  }
}
