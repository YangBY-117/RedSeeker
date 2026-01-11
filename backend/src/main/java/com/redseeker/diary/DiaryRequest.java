package com.redseeker.diary;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class DiaryRequest {
  @NotBlank
  private String title;
  private String content;
  private List<String> images;
  private List<String> tags;
  private Long attractionId;
  private Boolean checkedIn;
  private String checkInNote;
  private String template;

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

  public List<String> getImages() {
    return images;
  }

  public void setImages(List<String> images) {
    this.images = images;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public Long getAttractionId() {
    return attractionId;
  }

  public void setAttractionId(Long attractionId) {
    this.attractionId = attractionId;
  }

  public Boolean getCheckedIn() {
    return checkedIn;
  }

  public void setCheckedIn(Boolean checkedIn) {
    this.checkedIn = checkedIn;
  }

  public String getCheckInNote() {
    return checkInNote;
  }

  public void setCheckInNote(String checkInNote) {
    this.checkInNote = checkInNote;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }
}
