package com.redseeker.recommend;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class RecommendBrowseRequest {
  @NotNull(message = "attractionId is required")
  @JsonProperty("attraction_id")
  private Long attractionId;

  public Long getAttractionId() {
    return attractionId;
  }

  public void setAttractionId(Long attractionId) {
    this.attractionId = attractionId;
  }
}
