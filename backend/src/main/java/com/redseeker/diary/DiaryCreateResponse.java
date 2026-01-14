package com.redseeker.diary;

public class DiaryCreateResponse {
  private Long id;
  private String title;
  private String createdAt;

  public DiaryCreateResponse() {
  }

  public DiaryCreateResponse(Long id, String title, String createdAt) {
    this.id = id;
    this.title = title;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
}
