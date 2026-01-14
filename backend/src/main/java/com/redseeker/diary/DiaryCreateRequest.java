package com.redseeker.diary;

import java.util.ArrayList;
import java.util.List;

public class DiaryCreateRequest {
  private Long userId;
  private String title;
  private String content;
  private String destination;
  private String travelDate;
  private List<Long> attractionIds = new ArrayList<>();
  private List<DiaryMediaInput> mediaInputs = new ArrayList<>();

  public DiaryCreateRequest() {
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
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

  public List<Long> getAttractionIds() {
    return attractionIds;
  }

  public void setAttractionIds(List<Long> attractionIds) {
    this.attractionIds = attractionIds == null ? new ArrayList<>() : new ArrayList<>(attractionIds);
  }

  public List<DiaryMediaInput> getMediaInputs() {
    return mediaInputs;
  }

  public void setMediaInputs(List<DiaryMediaInput> mediaInputs) {
    this.mediaInputs = mediaInputs == null ? new ArrayList<>() : new ArrayList<>(mediaInputs);
  }
}
