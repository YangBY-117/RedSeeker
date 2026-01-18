package com.redseeker.diary;

public class DiaryComment {
  private Long id;
  private Long diaryId;
  private Long userId;
  private String username;
  private String content;
  private String createdAt;

  public DiaryComment() {}

  public DiaryComment(Long id, Long diaryId, Long userId, String username, String content, String createdAt) {
    this.id = id;
    this.diaryId = diaryId;
    this.userId = userId;
    this.username = username;
    this.content = content;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getDiaryId() {
    return diaryId;
  }

  public void setDiaryId(Long diaryId) {
    this.diaryId = diaryId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
}
