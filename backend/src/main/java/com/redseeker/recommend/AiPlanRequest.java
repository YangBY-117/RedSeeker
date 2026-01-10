package com.redseeker.recommend;

import jakarta.validation.constraints.NotBlank;

public class AiPlanRequest {
  @NotBlank(message = "prompt is required")
  private String prompt;
  private String city;
  private Integer days;

  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public Integer getDays() {
    return days;
  }

  public void setDays(Integer days) {
    this.days = days;
  }
}
