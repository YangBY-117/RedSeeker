package com.redseeker.route;

public class AttractionSummary {
  private Long id;
  private String name;
  private String address;
  private Double longitude;
  private Double latitude;
  private String stageName;
  private Integer stageStart;
  private Integer stageEnd;

  public AttractionSummary() {
  }

  public AttractionSummary(Long id, String name, String address, Double longitude, Double latitude) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.longitude = longitude;
    this.latitude = latitude;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
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

  public String getStageName() {
    return stageName;
  }

  public void setStageName(String stageName) {
    this.stageName = stageName;
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
}
