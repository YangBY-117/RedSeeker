package com.redseeker.diary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DiaryCommentRequest {
  @NotNull(message = "diaryId is required")
  private Long diaryId;

  @NotBlank(message = "content is required")
  private String content;

  public Long getDiaryId() {
    return diaryId;
  }

  public void setDiaryId(Long diaryId) {
    this.diaryId = diaryId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
