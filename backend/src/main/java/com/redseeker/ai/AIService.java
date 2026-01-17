package com.redseeker.ai;

public interface AIService {
  DiaryGenerateResponse generateDiaryContent(DiaryGenerateRequest request);
  ImageGenerateResponse generateImageFromText(TextToImageRequest request);
  AnimationGenerateResponse generateAnimationFromImages(ImageToAnimationRequest request);
}
