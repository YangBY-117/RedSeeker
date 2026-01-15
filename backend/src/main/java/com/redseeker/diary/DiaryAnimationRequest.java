package com.redseeker.diary;

import java.util.ArrayList;
import java.util.List;

public class DiaryAnimationRequest {
  private List<String> images = new ArrayList<>();
  private String description;

  public List<String> getImages() {
    return images;
  }

  public void setImages(List<String> images) {
    this.images = images;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
