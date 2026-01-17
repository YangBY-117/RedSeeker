package com.redseeker.ai;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ImageToAnimationRequest {
  @NotEmpty
  private List<String> images;
  
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
