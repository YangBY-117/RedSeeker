package com.redseeker.user;

public class UserBrowseResponse {
  private Long id;
  private Long userId;
  private Long attractionId;
  private String browseTime;

  public UserBrowseResponse() {
  }

  public UserBrowseResponse(Long id, Long userId, Long attractionId, String browseTime) {
    this.id = id;
    this.userId = userId;
    this.attractionId = attractionId;
    this.browseTime = browseTime;
  }

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
