package com.redseeker.place;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Place {
  private String id;
  private String name;
  private String address;
  private String category;
  private String history;
  private List<String> periods = new ArrayList<>();

  public Place() {
  }

  public Place(String id, String name, String address, String category, String history,
      List<String> periods) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.category = category;
    this.history = history;
    if (periods != null) {
      this.periods = new ArrayList<>(periods);
    }
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

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getHistory() {
    return history;
  }

  public void setHistory(String history) {
    this.history = history;
  }

  public List<String> getPeriods() {
    return Collections.unmodifiableList(periods);
  }

  public void setPeriods(List<String> periods) {
    this.periods = periods == null ? new ArrayList<>() : new ArrayList<>(periods);
  }
}
