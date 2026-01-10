package com.redseeker.recommend;

import java.util.List;

public class ItineraryPlan {
  private int day;
  private String title;
  private String description;
  private List<String> activities;

  public ItineraryPlan() {
  }

  public ItineraryPlan(int day, String title, String description, List<String> activities) {
    this.day = day;
    this.title = title;
    this.description = description;
    this.activities = activities;
  }

  public int getDay() {
    return day;
  }

  public void setDay(int day) {
    this.day = day;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getActivities() {
    return activities;
  }

  public void setActivities(List<String> activities) {
    this.activities = activities;
  }
}
