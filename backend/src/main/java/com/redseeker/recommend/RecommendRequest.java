package com.redseeker.recommend;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class RecommendRequest {
  @NotBlank(message = "city is required")
  private String city;
  private Long userId;
  private List<String> preferences;

  private String travelStyle;
  private Integer days;
  private Double userLongitude;
  private Double userLatitude;
  private String visitTime;

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
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

  public Double getUserLongitude() {
    return userLongitude;
  }

  public void setUserLongitude(Double userLongitude) {
    this.userLongitude = userLongitude;
  }

  public Double getUserLatitude() {
    return userLatitude;
  }

  public void setUserLatitude(Double userLatitude) {
    this.userLatitude = userLatitude;
  }

  public String getVisitTime() {
    return visitTime;
  }

  public void setVisitTime(String visitTime) {
    this.visitTime = visitTime;
  }
}
