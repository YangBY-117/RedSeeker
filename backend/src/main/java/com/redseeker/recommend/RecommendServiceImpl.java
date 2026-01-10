package com.redseeker.recommend;

import com.redseeker.common.ServiceException;
import com.redseeker.common.ErrorCode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecommendServiceImpl implements RecommendService {
  private static final List<RecommendItem> CATALOG = List.of(
      new RecommendItem(
          "中国共产党第一次全国代表大会会址",
          "革命旧址",
          0.95,
          "中国共产党的诞生地，位于上海法租界望志路106号（今兴业路76号）。",
          "见证建党伟业的核心地标，必访之地。"
      ),
      new RecommendItem(
          "井冈山革命博物馆",
          "博物馆",
          0.90,
          "纪念井冈山革命根据地斗争历史的综合性博物馆。",
          "全面了解农村包围城市道路起点的最佳场所。"
      ),
      new RecommendItem(
          "延安革命纪念馆",
          "纪念馆",
          0.92,
          "展示中共中央在延安十三年领导中国革命的光辉历程。",
          "延安精神的发源地，深刻感受艰苦奋斗精神。"
      ),
      new RecommendItem(
          "遵义会议会址",
          "重要会议",
          0.93,
          "1935年召开的遵义会议挽救了党、挽救了红军、挽救了中国革命。",
          "中国革命史上的转折点，具有极高的历史价值。"
      ),
      new RecommendItem(
          "西柏坡纪念馆",
          "纪念馆",
          0.88,
          "解放战争时期中央工委、中共中央和解放军总部的所在地。",
          "新中国从这里走来，进京赶考的出发地。"
      )
  );

  @Override
  public List<RecommendItem> getRecommendations(RecommendRequest request) {
    if (request.getCity() == null || request.getCity().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "city is required");
    }
    
    // Simple mock logic: In a real app, this would query DB/Search Engine based on city/tags
    // Here we just return the catalog and maybe filter slightly or just randomize scores
    
    return CATALOG.stream()
        .map(item -> {
            // Clone item to modify score/reason dynamically
             return new RecommendItem(
                 item.getName(), 
                 item.getCategory(), 
                 calculateScore(item, request), 
                 item.getHistory(), 
                 generateReason(item, request)
             );
        })
        .sorted(Comparator.comparingDouble(RecommendItem::getScore).reversed())
        .collect(Collectors.toList());
  }

  private double calculateScore(RecommendItem item, RecommendRequest request) {
      double baseScore = item.getScore();
      if (request.getPreferences() != null && request.getPreferences().contains(item.getCategory())) {
          return Math.min(1.0, baseScore + 0.05);
      }
      return baseScore;
  }

  private String generateReason(RecommendItem item, RecommendRequest request) {
      if (request.getPreferences() != null && request.getPreferences().contains(item.getCategory())) {
          return "根据您的偏好强烈推荐：" + item.getReason();
      }
      return item.getReason();
  }

  @Override
  public AiPlanResponse generateAiPlan(AiPlanRequest request) {
      // Mock AI Plan generation
      AiPlanResponse response = new AiPlanResponse();
      response.setSummary("基于您提供的要求（" + request.getPrompt() + "），为您规划了如下红色研学行程。");
      
      List<ItineraryPlan> plans = new ArrayList<>();
      ItineraryPlan day1 = new ItineraryPlan();
      day1.setDay(1);
      day1.setTitle("追寻初心之旅");
      day1.setDescription("上午参观中共一大会址，下午游览附近的新天地，感受历史与现代的交融。");
      plans.add(day1);
      
      ItineraryPlan day2 = new ItineraryPlan();
      day2.setDay(2);
      day2.setTitle("深入研学体验");
      day2.setDescription("参观相关博物馆，进行深度学习和交流。");
      plans.add(day2);

      response.setPlans(plans);
      return response;
  }
}
