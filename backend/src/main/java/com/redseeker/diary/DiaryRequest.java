package com.redseeker.diary;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class DiaryRequest {
  @NotBlank
  private String title;
  private Long userId;
  private String content;
  private List<String> images;
  private Long attractionId;
  private Boolean publicEntry;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public List<String> getImages() {
    return images;
  }

  public void setImages(List<String> images) {
    this.images = images;
  }

  public Long getAttractionId() {
    return attractionId;
  }

  public void setAttractionId(Long attractionId) {
    this.attractionId = attractionId;
  }

  public Boolean getPublicEntry() {
    return publicEntry;
  }

  public void setPublicEntry(Boolean publicEntry) {
    this.publicEntry = publicEntry;
  }
}
