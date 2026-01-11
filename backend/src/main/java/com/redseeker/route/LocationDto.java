package com.redseeker.route;

import jakarta.validation.constraints.NotNull;

public class LocationDto {
  @NotNull
  private Double longitude;
  @NotNull
  private Double latitude;
  private String address;

  public LocationDto() {}

  public LocationDto(Double longitude, Double latitude, String address) {
    this.longitude = longitude;
    this.latitude = latitude;
    this.address = address;
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

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }
}
