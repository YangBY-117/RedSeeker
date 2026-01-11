package com.redseeker.route;

import java.util.List;

public class RouteStage {
  private String stageName;
  private String stagePeriod;
  private List<OrderedAttraction> attractions;

  public RouteStage(String stageName, String stagePeriod, List<OrderedAttraction> attractions) {
    this.stageName = stageName;
    this.stagePeriod = stagePeriod;
    this.attractions = attractions;
  }

  public String getStageName() {
    return stageName;
  }

  public String getStagePeriod() {
    return stagePeriod;
  }

  public List<OrderedAttraction> getAttractions() {
    return attractions;
  }
}
