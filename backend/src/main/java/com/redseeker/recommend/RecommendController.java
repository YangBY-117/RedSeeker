package com.redseeker.recommend;

import com.redseeker.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {
  private final RecommendService recommendService;

  public RecommendController(RecommendService recommendService) {
    this.recommendService = recommendService;
  }

  @PostMapping("/list")
  public ApiResponse<List<RecommendItem>> list(@Valid @RequestBody RecommendRequest request) {
    return ApiResponse.ok(recommendService.getRecommendations(request));
  }

  @PostMapping("/ai-plan")
  public ApiResponse<AiPlanResponse> aiPlan(@Valid @RequestBody AiPlanRequest request) {
    return ApiResponse.ok(recommendService.generateAiPlan(request));
  }
}
