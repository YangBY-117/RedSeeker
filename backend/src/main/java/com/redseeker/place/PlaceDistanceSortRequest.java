package com.redseeker.place;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class PlaceDistanceSortRequest {
  @NotNull
  @Valid
  private PlaceLocation origin;

  @NotEmpty
  @Valid
  private List<PlaceCandidate> places;

  @JsonProperty("transport_mode")
  private String transportMode;

  public PlaceLocation getOrigin() {
    return origin;
  }

  public void setOrigin(PlaceLocation origin) {
    this.origin = origin;
  }

  public List<PlaceCandidate> getPlaces() {
    return places;
  }

  public void setPlaces(List<PlaceCandidate> places) {
    this.places = places;
  }

  public String getTransportMode() {
    return transportMode;
  }

  public void setTransportMode(String transportMode) {
    this.transportMode = transportMode;
  }
}
