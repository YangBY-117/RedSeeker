package com.redseeker.diary;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiaryMedia {
  private Long id;

  @JsonProperty("media_type")
  private String mediaType;

  @JsonProperty("file_path")
  private String filePath;

  @JsonProperty("thumbnail_path")
  private String thumbnailPath;

  @JsonProperty("display_order")
  private Integer displayOrder;

  public DiaryMedia() {
  }

  public DiaryMedia(Long id, String mediaType, String filePath, String thumbnailPath, Integer displayOrder) {
    this.id = id;
    this.mediaType = mediaType;
    this.filePath = filePath;
    this.thumbnailPath = thumbnailPath;
    this.displayOrder = displayOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMediaType() {
    return mediaType;
  }

  public void setMediaType(String mediaType) {
    this.mediaType = mediaType;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getThumbnailPath() {
    return thumbnailPath;
  }

  public void setThumbnailPath(String thumbnailPath) {
    this.thumbnailPath = thumbnailPath;
  }

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
  }
}
