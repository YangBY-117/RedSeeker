package com.redseeker.route;

public interface RouteService {
  SingleRouteResponse planSingleRoute(SingleRouteRequest request);

  RoutePlanResult planMultipleRoute(MultipleRouteRequest request);

  RouteLocation getCurrentLocation();
}
