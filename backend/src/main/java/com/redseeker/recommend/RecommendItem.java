package com.redseeker.recommend;

import java.util.List;

public class RecommendItem {
  private String id;
  private String name;
  private String category; // e.g., "Revolutionary Site", "Museum"
  private List<String> tags;
  private double score; // Matching score (0.0 - 1.0)
  private String history; // Historical background
  private String reason; // Recommendation reason
  private String address; // Address
  private Double averageRating; // Average rating from database
  private Integer totalRatings; // Total number of ratings
  private Integer heatScore; // Heat score (browse count)

  public RecommendItem() {
  }

  public RecommendItem(String id, String name, String category, List<String> tags, double score, String history, String reason) {
    this.id = id;
    this.name = name;
    this.category = category;
    this.tags = tags;
    this.score = score;
    this.history = history;
    this.reason = reason;
  }

  public RecommendItem(String id, String name, String category, List<String> tags, double score, String history, String reason, String address, Double averageRating, Integer totalRatings, Integer heatScore) {
    this.id = id;
    this.name = name;
    this.category = category;
    this.tags = tags;
    this.score = score;
    this.history = history;
    this.reason = reason;
    this.address = address;
    this.averageRating = averageRating;
    this.totalRatings = totalRatings;
    this.heatScore = heatScore;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  public String getHistory() {
    return history;
  }

  public void setHistory(String history) {
    this.history = history;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
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

  public Integer getHeatScore() {
    return heatScore;
  }

  public void setHeatScore(Integer heatScore) {
    this.heatScore = heatScore;
  }
}
