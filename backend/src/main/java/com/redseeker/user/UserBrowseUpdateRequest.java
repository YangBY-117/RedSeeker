package com.redseeker.user;

import jakarta.validation.constraints.NotNull;

public class UserBrowseUpdateRequest {
  @NotNull(message = "id is required")
  private Long id;

  @NotNull(message = "userId is required")
  private Long userId;

  @NotNull(message = "attractionId is required")
  private Long attractionId;

  private String browseTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public String getBrowseTime() {
    return browseTime;
  }

  public void setBrowseTime(String browseTime) {
    this.browseTime = browseTime;
  }
}
