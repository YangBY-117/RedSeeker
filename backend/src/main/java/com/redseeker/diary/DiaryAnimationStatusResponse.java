package com.redseeker.diary;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiaryAnimationStatusResponse {
  @JsonProperty("task_id")
  private String taskId;

  private String status;

  @JsonProperty("video_url")
  private String videoUrl;

  public DiaryAnimationStatusResponse() {
  }

  public DiaryAnimationStatusResponse(String taskId, String status, String videoUrl) {
    this.taskId = taskId;
    this.status = status;
    this.videoUrl = videoUrl;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getVideoUrl() {
    return videoUrl;
  }

  public void setVideoUrl(String videoUrl) {
    this.videoUrl = videoUrl;
  }
}
