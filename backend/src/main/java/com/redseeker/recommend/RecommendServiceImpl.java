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
      new RecommendItem("城市博物馆", "文化", 0.8),
      new RecommendItem("历史古街", "文化", 0.7),
      new RecommendItem("滨海步道", "自然", 0.65),
      new RecommendItem("城市公园", "休闲", 0.6),
      new RecommendItem("特色夜市", "美食", 0.75)
  );

  @Override
  public List<RecommendItem> getRecommendations(RecommendRequest request) {
    if (request.getCity() == null || request.getCity().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "city is required");
    }
    List<String> preferences = request.getPreferences() == null
        ? List.of()
        : request.getPreferences().stream()
            .filter(Objects::nonNull)
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(Collectors.toList());

    List<RecommendItem> scored = new ArrayList<>();
    for (RecommendItem item : CATALOG) {
      double score = item.getScore();
      String category = item.getCategory().toLowerCase(Locale.ROOT);
      if (!preferences.isEmpty() && preferences.contains(category)) {
        score += 0.2;
      }
      if (request.getTravelStyle() != null
          && request.getTravelStyle().toLowerCase(Locale.ROOT).contains(category)) {
        score += 0.1;
      }
      scored.add(new RecommendItem(item.getName(), item.getCategory(), score));
    }

    return scored.stream()
        .sorted(Comparator.comparingDouble(RecommendItem::getScore).reversed())
        .limit(5)
        .collect(Collectors.toList());
  }

  @Override
  public AiPlanResponse generateAiPlan(AiPlanRequest request) {
    if (request.getPrompt() == null || request.getPrompt().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "prompt is required");
    }
    int days = request.getDays() == null ? 3 : request.getDays();
    List<ItineraryPlan> plans = new ArrayList<>();
    for (int day = 1; day <= days; day++) {
      List<String> activities = List.of(
          "上午：根据偏好安排文化与景点游览",
          "下午：安排休闲体验与当地特色",
          "晚上：美食与夜景体验"
      );
      plans.add(new ItineraryPlan(day, activities));
    }
    String summary = String.format("AI 行程建议（%s）: %d 日游，涵盖文化与休闲体验。",
        request.getCity() == null ? "目的地" : request.getCity(), days);
    return new AiPlanResponse(summary, plans);
  }
}
