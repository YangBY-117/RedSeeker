package com.redseeker.place;

public class PlaceQueryCondition {
  private String keyword;
  private String region;
  private String event;
  private Integer minHeat;
  private Integer maxHeat;
  private String sortBy = "heat";
  private int limit = 10;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getEvent() {
    return event;
  }

  public void setEvent(String event) {
    this.event = event;
  }

  public Integer getMinHeat() {
    return minHeat;
  }

  public void setMinHeat(Integer minHeat) {
    this.minHeat = minHeat;
  }

  public Integer getMaxHeat() {
    return maxHeat;
  }

  public void setMaxHeat(Integer maxHeat) {
    this.maxHeat = maxHeat;
  }

  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }
}
