package com.redseeker.place;

import java.util.List;

public class PlaceSearchResponse {
  private List<PlaceInfo> places;
  private int total;

  public PlaceSearchResponse(List<PlaceInfo> places) {
    this.places = places;
    this.total = places.size();
  }

  public List<PlaceInfo> getPlaces() {
    return places;
  }

  public int getTotal() {
    return total;
  }
}
