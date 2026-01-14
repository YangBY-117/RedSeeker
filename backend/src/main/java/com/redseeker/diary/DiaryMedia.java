package com.redseeker.diary;

public class DiaryMedia {
  private Long id;
  private String mediaType;
  private String filePath;
  private String thumbnailPath;
  private int displayOrder;

  public DiaryMedia() {
  }

  public DiaryMedia(Long id, String mediaType, String filePath, String thumbnailPath,
      int displayOrder) {
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

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }
}
