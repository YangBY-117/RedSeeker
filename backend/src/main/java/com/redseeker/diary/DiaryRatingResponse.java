package com.redseeker.diary;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiaryRatingResponse {
  @JsonProperty("average_rating")
  private Double averageRating;

  @JsonProperty("total_ratings")
  private Integer totalRatings;

  public DiaryRatingResponse() {
  }

  public DiaryRatingResponse(Double averageRating, Integer totalRatings) {
    this.averageRating = averageRating;
    this.totalRatings = totalRatings;
  }

  public Double getAverageRating() {
    return averageRating;
  }

  public void setAverageRating(Double averageRating) {
    this.averageRating = averageRating;
  }

  public Integer getTotalRatings() {
    return totalRatings;
  }

  public void setTotalRatings(Integer totalRatings) {
    this.totalRatings = totalRatings;
  }
}
