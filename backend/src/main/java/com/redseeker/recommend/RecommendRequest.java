package com.redseeker.recommend;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class RecommendRequest {
  @NotBlank(message = "city is required")
  private String city;
  private List<String> preferences;
  private String travelStyle;
  private Integer days;

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public List<String> getPreferences() {
    return preferences;
  }

  public void setPreferences(List<String> preferences) {
    this.preferences = preferences;
  }

  public String getTravelStyle() {
    return travelStyle;
  }

  public void setTravelStyle(String travelStyle) {
    this.travelStyle = travelStyle;
  }

  public Integer getDays() {
    return days;
  }

  public void setDays(Integer days) {
    this.days = days;
  }
}
