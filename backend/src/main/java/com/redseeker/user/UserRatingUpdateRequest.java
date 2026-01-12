package com.redseeker.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UserRatingUpdateRequest {
  @NotNull(message = "id is required")
  private Long id;

  @NotNull(message = "userId is required")
  private Long userId;

  @NotNull(message = "rating is required")
  @Min(value = 1, message = "rating must be >= 1")
  @Max(value = 5, message = "rating must be <= 5")
  private Integer rating;

  private String comment;

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
