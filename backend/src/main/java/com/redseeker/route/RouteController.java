package com.redseeker.route;

import com.redseeker.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/route")
public class RouteController {
  private final RouteService routeService;

  public RouteController(RouteService routeService) {
    this.routeService = routeService;
  }

  @PostMapping("/single")
  public ApiResponse<SingleRouteResponse> planSingle(@Valid @RequestBody SingleRouteRequest request) {
    return ApiResponse.ok(routeService.planSingleRoute(request));
  }

  @PostMapping("/multiple")
  public ApiResponse<MultipleRouteResponse> planMultiple(@Valid @RequestBody MultipleRouteRequest request) {
    return ApiResponse.ok(routeService.planMultipleRoute(request));
  }

  @GetMapping("/current-location")
  public ApiResponse<RouteLocation> currentLocation() {
    return ApiResponse.ok(routeService.getCurrentLocation());
  }
}
