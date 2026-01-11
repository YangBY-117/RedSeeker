package com.redseeker.route;

import java.util.List;

public class AttractionInfo {
  private Long id;
  private String name;
  private String address;
  private double longitude;
  private double latitude;
  private String period;
  private List<String> storyPoints;
  private int startYear;

  public AttractionInfo() {}

  public AttractionInfo(Long id, String name, String address, double longitude, double latitude, String period,
      List<String> storyPoints, int startYear) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.longitude = longitude;
    this.latitude = latitude;
    this.period = period;
    this.storyPoints = storyPoints;
    this.startYear = startYear;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public List<String> getStoryPoints() {
    return storyPoints;
  }

  public void setStoryPoints(List<String> storyPoints) {
    this.storyPoints = storyPoints;
  }

  public int getStartYear() {
    return startYear;
  }

  public void setStartYear(int startYear) {
    this.startYear = startYear;
  }
}
