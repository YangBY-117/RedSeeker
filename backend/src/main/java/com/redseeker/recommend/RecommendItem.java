package com.redseeker.recommend;

public class RecommendItem {
  private String name;
  private String category;
  private double score;

  public RecommendItem() {
  }

  public RecommendItem(String name, String category, double score) {
    this.name = name;
    this.category = category;
    this.score = score;
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

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }
}
