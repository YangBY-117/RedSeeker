package com.redseeker.route;

import jakarta.validation.constraints.NotNull;

public class Point {
  @NotNull
  private Double longitude;
  @NotNull
  private Double latitude;

  public Point() {}

  public Point(Double longitude, Double latitude) {
    this.longitude = longitude;
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }
}
