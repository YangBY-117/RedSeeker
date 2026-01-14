package com.redseeker.recommend;

import com.redseeker.common.ApiResponse;
import com.redseeker.common.ErrorCode;
import com.redseeker.common.ServiceException;
import com.redseeker.user.UserBrowseRequest;
import com.redseeker.user.UserBrowseResponse;
import com.redseeker.user.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {
  private final RecommendService recommendService;
  private final UserService userService;

  public RecommendController(RecommendService recommendService, UserService userService) {
    this.recommendService = recommendService;
    this.userService = userService;
  }

  @PostMapping("/list")
  public ApiResponse<List<RecommendItem>> list(@Valid @RequestBody RecommendRequest request) {
    return ApiResponse.ok(recommendService.getRecommendations(request));
  }

  @PostMapping("/ai-plan")
  public ApiResponse<AiPlanResponse> aiPlan(@Valid @RequestBody AiPlanRequest request) {
    return ApiResponse.ok(recommendService.generateAiPlan(request));
  }

  @PostMapping("/browse")
  public ApiResponse<UserBrowseResponse> recordBrowse(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @Valid @RequestBody RecommendBrowseRequest request) {
    String token = extractBearerToken(authHeader);
    Long userId = userService.resolveUserId(token);
    if (userId == null) {
      throw new ServiceException(ErrorCode.UNAUTHORIZED, "Authorization token is required");
    }
    UserBrowseRequest browseRequest = new UserBrowseRequest();
    browseRequest.setUserId(userId);
    browseRequest.setAttractionId(request.getAttractionId());
    return ApiResponse.ok(userService.addBrowse(browseRequest));
  }

  private String extractBearerToken(String authHeader) {
    if (authHeader == null || authHeader.isBlank()) {
      return null;
    }
    if (authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    return authHeader;
  }
}
