package com.redseeker.route;

public interface RouteService {
  RouteSingleResponse planSingle(RouteSingleRequest request);

  RouteMultipleResponse planMultiple(RouteMultipleRequest request);

  LocationDto currentLocation();
}
