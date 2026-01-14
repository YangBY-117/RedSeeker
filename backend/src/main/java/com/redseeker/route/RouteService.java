package com.redseeker.route;

import java.util.List;

public interface RouteService {
  RouteResult calculateRoute(List<Point> points);
}
