package com.redseeker.ai;

import jakarta.validation.constraints.NotBlank;

public class TextToImageRequest {
  @NotBlank
  private String prompt;

  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }
}
