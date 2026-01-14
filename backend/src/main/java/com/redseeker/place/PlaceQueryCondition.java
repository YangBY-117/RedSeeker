package com.redseeker.place;

import java.util.ArrayList;
import java.util.List;

public class PlaceQueryCondition {
  private String province;
  private String historicalPeriod;
  private List<String> keywords = new ArrayList<>();

  public PlaceQueryCondition() {
  }

  public PlaceQueryCondition(String province, String historicalPeriod, List<String> keywords) {
    this.province = province;
    this.historicalPeriod = historicalPeriod;
    if (keywords != null) {
      this.keywords = new ArrayList<>(keywords);
    }
  }

  public String getProvince() {
    return province;
  }

  public void setProvince(String province) {
    this.province = province;
  }

  public String getHistoricalPeriod() {
    return historicalPeriod;
  }

  public void setHistoricalPeriod(String historicalPeriod) {
    this.historicalPeriod = historicalPeriod;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  public void setKeywords(List<String> keywords) {
    this.keywords = keywords == null ? new ArrayList<>() : new ArrayList<>(keywords);
  }
}
