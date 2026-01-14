package com.redseeker.diary;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DiaryRatingRequest {
  @NotNull(message = "userId is required")
  private Long userId;

  @NotNull(message = "rating is required")
  @Min(1)
  @Max(5)
  private Integer rating;

  public DiaryRatingRequest() {
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
}
