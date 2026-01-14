package com.redseeker.diary;

public class DiaryMediaInput {
  private String mediaType;
  private String filePath;
  private long fileSize;
  private String thumbnailPath;
  private int displayOrder;

  public DiaryMediaInput() {
  }

  public DiaryMediaInput(String mediaType, String filePath, long fileSize, String thumbnailPath,
      int displayOrder) {
    this.mediaType = mediaType;
    this.filePath = filePath;
    this.fileSize = fileSize;
    this.thumbnailPath = thumbnailPath;
    this.displayOrder = displayOrder;
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

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
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
