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
  private String businessHours; // Business hours
  private Double perCapitaConsumption; // Per capita consumption
  private Double averageRating; // Average rating from database
  private Integer totalRatings; // Total number of ratings
  private Integer heatScore; // Heat score based on browse count
  private Double longitude; // Longitude coordinate
  private Double latitude; // Latitude coordinate
  private Integer stageStart; // Historical stage start year
  private Integer stageEnd; // Historical stage end year
  private String stageName; // Historical period name

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

  public RecommendItem(String id, String name, String category, List<String> tags, double score, String history, String reason,
                       String address, String businessHours, Double perCapitaConsumption,
                       Double averageRating, Integer totalRatings, Integer heatScore) {
    this(id, name, category, tags, score, history, reason, address, businessHours, perCapitaConsumption,
         averageRating, totalRatings, heatScore, null, null, null, null, null);
  }

  public RecommendItem(String id, String name, String category, List<String> tags, double score, String history, String reason,
                       String address, String businessHours, Double perCapitaConsumption,
                       Double averageRating, Integer totalRatings, Integer heatScore,
                       Double longitude, Double latitude,
                       Integer stageStart, Integer stageEnd, String stageName) {
    this.id = id;
    this.name = name;
    this.category = category;
    this.tags = tags;
    this.score = score;
    this.history = history;
    this.reason = reason;
    this.address = address;
    this.businessHours = businessHours;
    this.perCapitaConsumption = perCapitaConsumption;
    this.averageRating = averageRating;
    this.totalRatings = totalRatings;
    this.heatScore = heatScore;
    this.longitude = longitude;
    this.latitude = latitude;
    this.stageStart = stageStart;
    this.stageEnd = stageEnd;
    this.stageName = stageName;
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

  public String getBusinessHours() {
    return businessHours;
  }

  public void setBusinessHours(String businessHours) {
    this.businessHours = businessHours;
  }

  public Double getPerCapitaConsumption() {
    return perCapitaConsumption;
  }

  public void setPerCapitaConsumption(Double perCapitaConsumption) {
    this.perCapitaConsumption = perCapitaConsumption;
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

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Integer getStageStart() {
    return stageStart;
  }

  public void setStageStart(Integer stageStart) {
    this.stageStart = stageStart;
  }

  public Integer getStageEnd() {
    return stageEnd;
  }

  public void setStageEnd(Integer stageEnd) {
    this.stageEnd = stageEnd;
  }

  public String getStageName() {
    return stageName;
  }

  public void setStageName(String stageName) {
    this.stageName = stageName;
  }
}
