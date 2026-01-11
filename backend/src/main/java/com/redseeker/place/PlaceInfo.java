package com.redseeker.place;

public class PlaceInfo {
  private Long id;
  private String name;
  private String address;
  private String region;
  private String event;
  private double longitude;
  private double latitude;
  private int heatScore;
  private double rating;

  public PlaceInfo(Long id, String name, String address, String region, String event, double longitude, double latitude,
      int heatScore, double rating) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.region = region;
    this.event = event;
    this.longitude = longitude;
    this.latitude = latitude;
    this.heatScore = heatScore;
    this.rating = rating;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public String getRegion() {
    return region;
  }

  public String getEvent() {
    return event;
  }

  public double getLongitude() {
    return longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public int getHeatScore() {
    return heatScore;
  }

  public double getRating() {
    return rating;
  }
}
