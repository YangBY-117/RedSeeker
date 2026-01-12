package com.redseeker.user;

public class UserProfileResponse {
  private Long id;
  private String username;
  private String createdAt;
  private String lastLogin;

  public UserProfileResponse() {
  }

  public UserProfileResponse(Long id, String username, String createdAt, String lastLogin) {
    this.id = id;
    this.username = username;
    this.createdAt = createdAt;
    this.lastLogin = lastLogin;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getLastLogin() {
    return lastLogin;
  }

  public void setLastLogin(String lastLogin) {
    this.lastLogin = lastLogin;
  }
}
