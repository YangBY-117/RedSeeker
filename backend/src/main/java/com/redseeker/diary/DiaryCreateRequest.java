package com.redseeker.diary;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class DiaryCreateRequest {
  @NotBlank
  private String title;

  @NotBlank
  private String content;

  private String destination;

  private String travelDate;

  private List<Long> attractionIds = new ArrayList<>();

  private List<MultipartFile> images = new ArrayList<>();

  private List<MultipartFile> videos = new ArrayList<>();

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public String getTravelDate() {
    return travelDate;
  }

  public void setTravelDate(String travelDate) {
    this.travelDate = travelDate;
  }

  public List<Long> getAttractionIds() {
    return attractionIds;
  }

  public void setAttractionIds(List<Long> attractionIds) {
    this.attractionIds = attractionIds;
  }

  public List<MultipartFile> getImages() {
    return images;
  }

  public void setImages(List<MultipartFile> images) {
    this.images = images;
  }

  public List<MultipartFile> getVideos() {
    return videos;
  }

  public void setVideos(List<MultipartFile> videos) {
    this.videos = videos;
  }
}
