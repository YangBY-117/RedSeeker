package com.redseeker.user;

public class UserRatingResponse {
  private Long id;
  private Long userId;
  private Long attractionId;
  private Integer rating;
  private String comment;
  private String createdAt;

  public UserRatingResponse() {
  }

  public UserRatingResponse(
      Long id,
      Long userId,
      Long attractionId,
      Integer rating,
      String comment,
      String createdAt) {
    this.id = id;
    this.userId = userId;
    this.attractionId = attractionId;
    this.rating = rating;
    this.comment = comment;
    this.createdAt = createdAt;
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

  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
}
