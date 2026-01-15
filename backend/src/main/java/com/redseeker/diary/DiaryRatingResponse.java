package com.redseeker.diary;

public class DiaryRatingResponse {
  private Double averageRating;
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
