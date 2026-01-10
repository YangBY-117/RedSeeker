package com.redseeker.recommend;

import java.util.List;

public interface RecommendService {
  List<RecommendItem> getRecommendations(RecommendRequest request);

  AiPlanResponse generateAiPlan(AiPlanRequest request);
}
