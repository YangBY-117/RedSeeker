package com.redseeker.diary;

import java.time.Instant;
import java.util.List;

public class DiaryEntry {
  private Long id;
  private String title;
  private String content;
  private List<String> images;
  private List<String> tags;
  private Long attractionId;
  private boolean checkedIn;
  private String checkInNote;
  private String template;
  private boolean shared;
  private Instant createdAt;
  private Instant updatedAt;

  public DiaryEntry(Long id, String title, String content, List<String> images, List<String> tags, Long attractionId,
      boolean checkedIn, String checkInNote, String template, boolean shared, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.images = images;
    this.tags = tags;
    this.attractionId = attractionId;
    this.checkedIn = checkedIn;
    this.checkInNote = checkInNote;
    this.template = template;
    this.shared = shared;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public List<String> getImages() {
    return images;
  }

  public List<String> getTags() {
    return tags;
  }

  public Long getAttractionId() {
    return attractionId;
  }

  public boolean isCheckedIn() {
    return checkedIn;
  }

  public String getCheckInNote() {
    return checkInNote;
  }

  public String getTemplate() {
    return template;
  }

  public boolean isShared() {
    return shared;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
