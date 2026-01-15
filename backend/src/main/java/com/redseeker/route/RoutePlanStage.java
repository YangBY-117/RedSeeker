package com.redseeker.route;

import java.util.ArrayList;
import java.util.List;

public class RoutePlanStage {
  private String stageName;
  private String stagePeriod;
  private List<RoutePlanStageAttraction> attractions = new ArrayList<>();

  public String getStageName() {
    return stageName;
  }

  public void setStageName(String stageName) {
    this.stageName = stageName;
  }

  public String getStagePeriod() {
    return stagePeriod;
  }

  public void setStagePeriod(String stagePeriod) {
    this.stagePeriod = stagePeriod;
  }

  public List<RoutePlanStageAttraction> getAttractions() {
    return attractions;
  }

  public void setAttractions(List<RoutePlanStageAttraction> attractions) {
    this.attractions = attractions;
  }
}
