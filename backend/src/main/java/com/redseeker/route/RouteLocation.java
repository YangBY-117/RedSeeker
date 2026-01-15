package com.redseeker.route;

import jakarta.validation.constraints.NotNull;

public class RouteLocation {
  @NotNull
  private Double longitude;

  @NotNull
  private Double latitude;

  private String address;

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

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }
}
