package com.redseeker.place;

import jakarta.validation.constraints.NotNull;

public class PlaceLocation {
  @NotNull
  private Double longitude;

  @NotNull
  private Double latitude;

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
