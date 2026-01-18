package com.redseeker.user;

import jakarta.validation.constraints.NotNull;

public class UserProfileUpdateRequest {
  @NotNull(message = "userId is required")
  private Long userId;
  private String username;
  private String password;
  private String avatar;

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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }
}
