package com.redseeker.diary;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiaryAnimationResponse {
  @JsonProperty("task_id")
  private String taskId;

  private String status;

  @JsonProperty("estimated_time")
  private Integer estimatedTime;

  public DiaryAnimationResponse() {
  }

  public DiaryAnimationResponse(String taskId, String status, Integer estimatedTime) {
    this.taskId = taskId;
    this.status = status;
    this.estimatedTime = estimatedTime;
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

  public Integer getEstimatedTime() {
    return estimatedTime;
  }

  public void setEstimatedTime(Integer estimatedTime) {
    this.estimatedTime = estimatedTime;
  }
}
