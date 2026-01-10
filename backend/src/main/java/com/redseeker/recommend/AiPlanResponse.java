package com.redseeker.recommend;

import java.util.List;

public class AiPlanResponse {
  private String summary;
  private List<ItineraryPlan> plans;

  public AiPlanResponse() {
  }

  public AiPlanResponse(String summary, List<ItineraryPlan> plans) {
    this.summary = summary;
    this.plans = plans;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public List<ItineraryPlan> getPlans() {
    return plans;
  }

  public void setPlans(List<ItineraryPlan> plans) {
    this.plans = plans;
  }
}
