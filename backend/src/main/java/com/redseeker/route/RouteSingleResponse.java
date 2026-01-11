package com.redseeker.route;

public class RouteSingleResponse {
  private RoutePath route;
  private AttractionInfo attraction;
  private LocationDto startLocation;

  public RouteSingleResponse(RoutePath route, AttractionInfo attraction, LocationDto startLocation) {
    this.route = route;
    this.attraction = attraction;
    this.startLocation = startLocation;
  }

  public RoutePath getRoute() {
    return route;
  }

  public AttractionInfo getAttraction() {
    return attraction;
  }

  public LocationDto getStartLocation() {
    return startLocation;
  }
}
