package com.redseeker.recommend;

import com.redseeker.common.ErrorCode;
import com.redseeker.common.ServiceException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecommendServiceImpl implements RecommendService {

  private final AiService aiService;

  public RecommendServiceImpl(AiService aiService) {
    this.aiService = aiService;
  }
  
  // 模拟数据库中的景点数据
  private static final List<RecommendItem> CATALOG = List.of(
      new RecommendItem(
          "1",
          "中共一大纪念馆",
          "3",
          List.of("建党", "上海", "纪念馆", "革命"),
          0.95,
          "中国共产党第一次全国代表大会会址，中国革命的起源地。",
          "建党初心的见证地，适合深入了解党史。"
      ),
      new RecommendItem(
          "2",
          "南湖革命纪念馆",
          "1",
          List.of("南湖", "红船", "纪念馆", "革命"),
          0.90,
          "纪念中共一大在南湖红船上闭幕的历史事件。",
          "沉浸式了解红船精神的重要场所。"
      ),
      new RecommendItem(
          "3",
          "中共二大会址纪念馆",
          "3",
          List.of("会议", "纪念馆", "革命", "上海"),
          0.92,
          "中国共产党第二次全国代表大会会址。",
          "适合系统了解党的早期组织发展历程。"
      ),
      new RecommendItem(
          "4",
          "广州农民运动讲习所旧址",
          "3",
          List.of("农民运动", "旧址", "培训", "广州"),
          0.93,
          "培养农民运动干部的重要场所。",
          "感受早期革命干部培养历程的历史旧址。"
      ),
      new RecommendItem(
          "5",
          "安源路矿工人运动纪念馆",
          "1",
          List.of("工人运动", "纪念馆", "安源", "革命"),
          0.88,
          "中国共产党领导工人运动的最早实践地之一。",
          "了解工人运动历史的经典红色地标。"
      )
  );

  // 模拟用户评分数据 (UserId -> (ItemId -> Rating))
  // 评分范围 1.0 - 5.0
  private static final Map<Long, Map<String, Double>> MOCK_USER_RATINGS = new HashMap<>();

  static {
    // 用户 101：偏好早期革命地 (上海一大会址, 井冈山)
    MOCK_USER_RATINGS.put(101L, Map.of("1", 5.0, "2", 4.5, "5", 3.0));
    // 用户 102：偏好纪念馆 (延安, 西柏坡)
    MOCK_USER_RATINGS.put(102L, Map.of("3", 5.0, "5", 4.8, "1", 3.5));
    // 用户 103：全面覆盖，高评分
    MOCK_USER_RATINGS.put(103L, Map.of("1", 4.0, "2", 4.0, "3", 4.0, "4", 4.0));
  }

  @Override
  public List<RecommendItem> getRecommendations(RecommendRequest request) {
    if (request.getCity() == null || request.getCity().isBlank()) {
      throw new ServiceException(ErrorCode.VALIDATION_ERROR, "city is required");
    }

    return CATALOG.stream()
        .map(item -> {
            // 计算综合评分
            double finalScore = calculateHybridScore(item, request);
            String reason = generateSmartReason(item, request, finalScore);
            
            return new RecommendItem(
                 item.getId(),
                 item.getName(), 
                 item.getCategory(),
                 item.getTags(),
                 finalScore, 
                 item.getHistory(), 
                 reason
             );
        })
        .sorted(Comparator.comparingDouble(RecommendItem::getScore).reversed())
        .collect(Collectors.toList());
  }

  private double calculateHybridScore(RecommendItem item, RecommendRequest request) {
      // 1. 基础热度分 (0.0 - 1.0)
      double baseScore = item.getScore();
      
      // 2. 基于内容的推荐 (Content-Based Filtering)
      double contentScore = calculateContentScore(item, request.getPreferences());
      
      // 3. 协同过滤推荐 (Collaborative Filtering)
      double cfScore = 0.0;
      if (request.getUserId() != null) {
          cfScore = calculateUserBasedCFScore(request.getUserId(), item.getId());
      }
      
      // 综合加权: 
      // 如果没有用户ID(游客模式)，主要依赖内容匹配和基础热度
      // 如果有用户ID，引入协同过滤
      if (request.getUserId() == null) {
          return (baseScore * 0.4) + (contentScore * 0.6);
      } else {
          return (baseScore * 0.2) + (contentScore * 0.4) + (cfScore * 0.4); 
      }
  }

  // --- 基于内容的推荐算法 ---
  private double calculateContentScore(RecommendItem item, List<String> preferences) {
      if (preferences == null || preferences.isEmpty()) {
          return 0.5; // 无偏好时给个中间分
      }
      
      long matchCount = 0;
      // 匹配分类
      if (preferences.contains(item.getCategory())) {
          matchCount += 2; // 分类匹配权重高
      }
      // 匹配标签
      for (String tag : item.getTags()) {
          if (preferences.contains(tag)) {
              matchCount += 1;
          }
      }
      
      // 简单的归一化: 假设最大匹配度为 5
      return Math.min(1.0, matchCount / 5.0);
  }

  // --- 基于用户的协同过滤算法 (User-Based CF) ---
  private double calculateUserBasedCFScore(Long targetUserId, String targetItemId) {
      Map<String, Double> targetUserRatings = MOCK_USER_RATINGS.get(targetUserId);
      if (targetUserRatings == null || targetUserRatings.isEmpty()) {
          return 0.5; // 冷启动用户
      }
      
      // 如果用户已经评价过该物品，直接不推荐或者给低分？
      // 这里假设我们要推荐用户没去过的，或者用户可能想重温的。
      // 为简化，如果评分过，直接返回其评分归一化值，表示"很符合该用户口味"
      if (targetUserRatings.containsKey(targetItemId)) {
          return targetUserRatings.get(targetItemId) / 5.0;
      }

      double weightedSum = 0.0;
      double similaritySum = 0.0;

      for (Map.Entry<Long, Map<String, Double>> entry : MOCK_USER_RATINGS.entrySet()) {
          Long otherUserId = entry.getKey();
          if (otherUserId.equals(targetUserId)) continue;

          Map<String, Double> otherUserRatings = entry.getValue();
          
          // 计算用户相似度 (使用余弦相似度)
          double similarity = calculateCosineSimilarity(targetUserRatings, otherUserRatings);
          
          if (similarity > 0 && otherUserRatings.containsKey(targetItemId)) {
              weightedSum += similarity * otherUserRatings.get(targetItemId);
              similaritySum += similarity;
          }
      }

      if (similaritySum == 0) {
          return 0.5; // 无法预测
      }

      // 预测评分 (1-5) -> 归一化 (0.2-1.0)
      double predictedRating = weightedSum / similaritySum;
      return predictedRating / 5.0; 
  }

  private double calculateCosineSimilarity(Map<String, Double> ratings1, Map<String, Double> ratings2) {
      double dotProduct = 0.0;
      double norm1 = 0.0;
      double norm2 = 0.0;

      // 找共同评价过的物品
      for (String itemId : ratings1.keySet()) {
          if (ratings2.containsKey(itemId)) {
              dotProduct += ratings1.get(itemId) * ratings2.get(itemId);
          }
          norm1 += Math.pow(ratings1.get(itemId), 2);
      }
      for (Double rating : ratings2.values()) {
            norm2 += Math.pow(rating, 2);
      }

      if (norm1 == 0 || norm2 == 0) return 0.0;
      return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
  }

  private String generateSmartReason(RecommendItem item, RecommendRequest request, double score) {
      if (score > 0.85) {
          List<String> hitPrefs = new ArrayList<>();
          if (request.getPreferences() != null) {
              for (String p : request.getPreferences()) {
                  if (item.getCategory().contains(p) || item.getTags().contains(p)) {
                      hitPrefs.add(p);
                  }
              }
          }
          
          if (!hitPrefs.isEmpty()) {
              return "根据您对 " + String.join("、", hitPrefs) + " 的兴趣强烈推荐。";
          }
          if (request.getUserId() != null && MOCK_USER_RATINGS.containsKey(request.getUserId())) {
             return "根据相似用户的喜好为您精选。";
          }
      }
      return item.getReason();
  }

  @Override
  public AiPlanResponse generateAiPlan(AiPlanRequest request) {
      // 构建提示词
      String prompt = String.format(
          "请为我设计一个红色旅游研学行程。城市：%s。天数：%d天。需求：%s。\n" +
          "请直接返回一段纯文本的行程描述，无需JSON格式。",
          request.getCity() != null ? request.getCity() : "如果不指定则默认上海",
          request.getDays() != null ? request.getDays() : 2,
          request.getPrompt()
      );

      // 调用AI
      String aiOutput = aiService.generateContent(prompt);

      // 解析或封装结果
      AiPlanResponse response = new AiPlanResponse();
      response.setSummary("AI为您生成的个性化方案：");
      
      // 这里为了简单展示，将AI的全文放在第一天的描述中，或者尝试简单切分
      // 在实际生产中，应要求AI返回JSON格式以便精确解析
      
      List<ItineraryPlan> plans = new ArrayList<>();
      ItineraryPlan plan = new ItineraryPlan();
      plan.setDay(1);
      plan.setTitle("AI定制行程");
      plan.setDescription(aiOutput); // 将AI返回的全部文本放入
      plans.add(plan);

      response.setPlans(plans);
      return response;
  }
}
