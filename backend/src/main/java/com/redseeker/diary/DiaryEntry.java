package com.redseeker.diary;

import java.time.Instant;
import java.util.List;

public class DiaryEntry {
  private Long id;
  private Long userId;
  private String title;
  private String content;
  private List<String> images;
  private Long attractionId;
  private boolean publicEntry;
  private Instant createdAt;
  private Instant updatedAt;

  public DiaryEntry(Long id, Long userId, String title, String content, List<String> images, Long attractionId,
      boolean publicEntry, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.userId = userId;
    this.title = title;
    this.content = content;
    this.images = images;
    this.attractionId = attractionId;
    this.publicEntry = publicEntry;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
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

  public Long getAttractionId() {
    return attractionId;
  }

  public boolean isPublicEntry() {
    return publicEntry;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
