package com.redseeker.ai;

import com.redseeker.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AIController {
  private final AIService aiService;

  public AIController(AIService aiService) {
    this.aiService = aiService;
  }

  @PostMapping("/generate-diary")
  public ApiResponse<DiaryGenerateResponse> generateDiary(@Valid @RequestBody DiaryGenerateRequest request) {
    return ApiResponse.ok(aiService.generateDiaryContent(request));
  }

  @PostMapping("/text-to-image")
  public ApiResponse<ImageGenerateResponse> textToImage(@Valid @RequestBody TextToImageRequest request) {
    return ApiResponse.ok(aiService.generateImageFromText(request));
  }

  @PostMapping("/image-to-animation")
  public ApiResponse<AnimationGenerateResponse> imageToAnimation(@Valid @RequestBody ImageToAnimationRequest request) {
    return ApiResponse.ok(aiService.generateAnimationFromImages(request));
  }
}
