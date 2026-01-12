package com.redseeker.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UserRatingRequest {
  @NotNull(message = "userId is required")
  private Long userId;

  @NotNull(message = "attractionId is required")
  private Long attractionId;

  @NotNull(message = "rating is required")
  @Min(value = 1, message = "rating must be >= 1")
  @Max(value = 5, message = "rating must be <= 5")
  private Integer rating;

  private String comment;

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
}
