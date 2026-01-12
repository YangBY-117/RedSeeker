package com.redseeker.user;

import jakarta.validation.constraints.NotNull;

public class UserBrowseRequest {
  @NotNull(message = "userId is required")
  private Long userId;

  @NotNull(message = "attractionId is required")
  private Long attractionId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getAttractionId() {
    return attractionId;
  }

  public void setAttractionId(Long attractionId) {
    this.attractionId = attractionId;
  }
}
