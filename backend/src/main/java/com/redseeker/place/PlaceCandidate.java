package com.redseeker.place;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PlaceCandidate {
  @NotBlank
  private String id;

  @NotBlank
  private String name;

  private String address;

  @NotNull
  @Valid
  private PlaceLocation location;

  private Integer distance;

  private Integer realDistance;

  private Integer realDuration;

  private String type;

  private String tel;

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

  public PlaceLocation getLocation() {
    return location;
  }

  public void setLocation(PlaceLocation location) {
    this.location = location;
  }

  public Integer getDistance() {
    return distance;
  }

  public void setDistance(Integer distance) {
    this.distance = distance;
  }

  public Integer getRealDistance() {
    return realDistance;
  }

  public void setRealDistance(Integer realDistance) {
    this.realDistance = realDistance;
  }

  public Integer getRealDuration() {
    return realDuration;
  }

  public void setRealDuration(Integer realDuration) {
    this.realDuration = realDuration;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTel() {
    return tel;
  }

  public void setTel(String tel) {
    this.tel = tel;
  }
}
