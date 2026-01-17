package com.redseeker.ai;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class DiaryGenerateRequest {
  @NotBlank
  private String prompt;
  
  private String destination;
  private String travelDate;
  private List<Long> attractionIds;

  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public String getTravelDate() {
    return travelDate;
  }

  public void setTravelDate(String travelDate) {
    this.travelDate = travelDate;
  }

  public List<Long> getAttractionIds() {
    return attractionIds;
  }

  public void setAttractionIds(List<Long> attractionIds) {
    this.attractionIds = attractionIds;
  }
}
