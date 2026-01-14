package com.redseeker.diary;

public class DiarySummary {
  private Long id;
  private String title;
  private String content;
  private String destination;
  private String travelDate;
  private int viewCount;
  private double averageRating;
  private int totalRatings;
  private DiaryAuthor author;
  private String coverImage;
  private String createdAt;
  private double recommendScore;

  public DiarySummary() {
  }

  public DiarySummary(Long id, String title, String content, String destination,
      String travelDate, int viewCount, double averageRating, int totalRatings,
      DiaryAuthor author, String coverImage, String createdAt, double recommendScore) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.destination = destination;
    this.travelDate = travelDate;
    this.viewCount = viewCount;
    this.averageRating = averageRating;
    this.totalRatings = totalRatings;
    this.author = author;
    this.coverImage = coverImage;
    this.createdAt = createdAt;
    this.recommendScore = recommendScore;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public String getTravelDate() {
    return travelDate;
  }

  public void setTravelDate(String travelDate) {
    this.travelDate = travelDate;
  }

  public int getViewCount() {
    return viewCount;
  }

  public void setViewCount(int viewCount) {
    this.viewCount = viewCount;
  }

  public double getAverageRating() {
    return averageRating;
  }

  public void setAverageRating(double averageRating) {
    this.averageRating = averageRating;
  }

  public int getTotalRatings() {
    return totalRatings;
  }

  public void setTotalRatings(int totalRatings) {
    this.totalRatings = totalRatings;
  }

  public DiaryAuthor getAuthor() {
    return author;
  }

  public void setAuthor(DiaryAuthor author) {
    this.author = author;
  }

  public String getCoverImage() {
    return coverImage;
  }

  public void setCoverImage(String coverImage) {
    this.coverImage = coverImage;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public double getRecommendScore() {
    return recommendScore;
  }

  public void setRecommendScore(double recommendScore) {
    this.recommendScore = recommendScore;
  }
}
