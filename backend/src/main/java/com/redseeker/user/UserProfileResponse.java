package com.redseeker.user;

public class UserProfileResponse {
  private Long id;
  private String username;
  private String createdAt;
  private String lastLogin;
  private String avatar;
  private Boolean isAdmin;

  public UserProfileResponse() {
  }

  public UserProfileResponse(Long id, String username, String createdAt, String lastLogin) {
    this.id = id;
    this.username = username;
    this.createdAt = createdAt;
    this.lastLogin = lastLogin;
  }

  public UserProfileResponse(Long id, String username, String createdAt, String lastLogin, String avatar, Boolean isAdmin) {
    this.id = id;
    this.username = username;
    this.createdAt = createdAt;
    this.lastLogin = lastLogin;
    this.avatar = avatar;
    this.isAdmin = isAdmin;
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

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public Boolean getIsAdmin() {
    return isAdmin;
  }

  public void setIsAdmin(Boolean isAdmin) {
    this.isAdmin = isAdmin;
  }
}
