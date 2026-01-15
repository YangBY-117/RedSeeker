package com.redseeker.route;

public interface RouteService {
  SingleRouteResponse planSingleRoute(SingleRouteRequest request);

  MultipleRouteResponse planMultipleRoute(MultipleRouteRequest request);

  RouteLocation getCurrentLocation();
}
