package com.redseeker.diary;

import java.util.ArrayList;
import java.util.List;

public class DiaryDetail {
  private Long id;
  private String title;
  private String content;
  private String destination;
  private String travelDate;
  private int viewCount;
  private double averageRating;
  private int totalRatings;
  private DiaryAuthor author;
  private List<DiaryMedia> media = new ArrayList<>();
  private List<DiaryAttraction> attractions = new ArrayList<>();
  private Integer userRating;
  private String createdAt;
  private String updatedAt;

  public DiaryDetail() {
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

  public List<DiaryMedia> getMedia() {
    return media;
  }

  public void setMedia(List<DiaryMedia> media) {
    this.media = media == null ? new ArrayList<>() : new ArrayList<>(media);
  }

  public List<DiaryAttraction> getAttractions() {
    return attractions;
  }

  public void setAttractions(List<DiaryAttraction> attractions) {
    this.attractions = attractions == null ? new ArrayList<>() : new ArrayList<>(attractions);
  }

  public Integer getUserRating() {
    return userRating;
  }

  public void setUserRating(Integer userRating) {
    this.userRating = userRating;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }
}
