package com.redseeker.route;

public interface RouteService {
  RouteSingleResponse planSingle(RouteSingleRequest request);

  RouteMultipleResponse planMultiple(RouteMultipleRequest request);

  RouteResult calculateRoute(java.util.List<Point> points);

  LocationDto currentLocation();
}
