package com.redseeker.place;

import com.redseeker.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/route")
public class RoutePlanController {
    private final RoutePlanService routePlanService;

    public RoutePlanController(RoutePlanService routePlanService) {
        this.routePlanService = routePlanService;
    }

    @PostMapping("/plan")
    public ApiResponse<RoutePlanResponse> planRoute(@Valid @RequestBody RoutePlanRequest request) {
        return ApiResponse.ok(routePlanService.planRoute(request));
    }

    // 注意：/multiple 端点已由 RouteController 处理，此处不再处理
    // 如需使用此Controller，请使用 /plan 端点
}
