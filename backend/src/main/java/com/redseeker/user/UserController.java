package com.redseeker.user;

import com.redseeker.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/user")
public class UserController {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  public ApiResponse<LoginResponse> register(@Valid @RequestBody UserRegisterRequest request) {
    userService.register(request);
    UserLoginRequest loginRequest = new UserLoginRequest();
    loginRequest.setUsername(request.getUsername());
    loginRequest.setPassword(request.getPassword());
    return ApiResponse.ok(userService.login(loginRequest));
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
    return ApiResponse.ok(userService.login(request));
  }

  @GetMapping("/profile")
  public ApiResponse<UserProfileResponse> profile(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam("userId") @NotNull Long userId) {
    String token = extractToken(authHeader);
    userService.assertAuthorized(token, userId);
    return ApiResponse.ok(userService.getProfile(userId));
  }

  @PutMapping("/profile")
  public ApiResponse<UserProfileResponse> updateProfile(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @Valid @RequestBody UserProfileUpdateRequest request) {
    String token = extractToken(authHeader);
    userService.assertAuthorized(token, request.getUserId());
    return ApiResponse.ok(userService.updateProfile(request));
  }

  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<String> uploadAvatar(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam("userId") @NotNull Long userId,
      @RequestParam("avatar") MultipartFile avatarFile) {
    String token = extractToken(authHeader);
    userService.assertAuthorized(token, userId);
    
    if (avatarFile == null || avatarFile.isEmpty()) {
      throw new com.redseeker.common.ServiceException(
          com.redseeker.common.ErrorCode.VALIDATION_ERROR, "Avatar file is required");
    }
    
    // 验证文件类型
    String contentType = avatarFile.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new com.redseeker.common.ServiceException(
          com.redseeker.common.ErrorCode.VALIDATION_ERROR, "Only image files are allowed");
    }
    
    // 保存文件
    try {
      String extension = getFileExtension(avatarFile.getOriginalFilename());
      String filename = "avatar_" + userId + "_" + System.currentTimeMillis() + extension;
      // 使用绝对路径，确保文件存储在正确的位置
      Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "avatars").toAbsolutePath().normalize();
      Files.createDirectories(uploadDir);
      Path target = uploadDir.resolve(filename);
      Files.write(target, avatarFile.getBytes());
      LOGGER.info("头像文件已保存: {}", target);
      LOGGER.info("头像文件是否存在: {}", Files.exists(target));
      
      // 返回文件URL
      String avatarUrl = "/uploads/avatars/" + filename;
      LOGGER.info("返回的头像URL: {}", avatarUrl);
      
      // 更新用户头像
      UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest();
      updateRequest.setUserId(userId);
      updateRequest.setAvatar(avatarUrl);
      userService.updateProfile(updateRequest);
      
      return ApiResponse.ok(avatarUrl);
    } catch (IOException ex) {
      LOGGER.error("上传头像失败: userId={}", userId, ex);
      throw new com.redseeker.common.ServiceException(
          com.redseeker.common.ErrorCode.INTERNAL_ERROR, "Failed to upload avatar: " + ex.getMessage());
    } catch (Exception ex) {
      LOGGER.error("上传头像时发生未知错误: userId={}", userId, ex);
      throw new com.redseeker.common.ServiceException(
          com.redseeker.common.ErrorCode.INTERNAL_ERROR, "Failed to upload avatar: " + ex.getMessage());
    }
  }

  private String getFileExtension(String filename) {
    if (filename == null) {
      return ".jpg";
    }
    int index = filename.lastIndexOf('.');
    return index >= 0 ? filename.substring(index) : ".jpg";
  }

  private String extractToken(String authHeader) {
    if (authHeader == null || authHeader.isBlank()) {
      return null;
    }
    return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
  }

  @PostMapping("/browse")
  public ApiResponse<UserBrowseResponse> addBrowse(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @Valid @RequestBody UserBrowseRequest request) {
    String token = extractToken(authHeader);
    userService.assertAuthorized(token, request.getUserId());
    return ApiResponse.ok(userService.addBrowse(request));
  }

  @GetMapping("/browse")
  public ApiResponse<List<UserBrowseResponse>> listBrowse(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam("userId") @NotNull Long userId) {
    String token = extractToken(authHeader);
    userService.assertAuthorized(token, userId);
    return ApiResponse.ok(userService.listBrowse(userId));
  }

  @PutMapping("/browse")
  public ApiResponse<UserBrowseResponse> updateBrowse(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @Valid @RequestBody UserBrowseUpdateRequest request) {
    String token = extractToken(authHeader);
    userService.assertAuthorized(token, request.getUserId());
    return ApiResponse.ok(userService.updateBrowse(request));
  }

  @DeleteMapping("/browse/{id}")
  public ApiResponse<Boolean> deleteBrowse(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @PathVariable("id") Long id,
      @RequestParam("userId") @NotNull Long userId) {
    String token = extractToken(authHeader);
    userService.assertAuthorized(token, userId);
    userService.deleteBrowse(id, userId);
    return ApiResponse.ok(true);
  }

  @PostMapping("/ratings")
  public ApiResponse<UserRatingResponse> addRating(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @Valid @RequestBody UserRatingRequest request) {
    String token = extractToken(authHeader);
    userService.assertAuthorized(token, request.getUserId());
    return ApiResponse.ok(userService.addRating(request));
  }

  @GetMapping("/ratings")
  public ApiResponse<List<UserRatingResponse>> listRatings(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam("userId") @NotNull Long userId) {
    String token = extractToken(authHeader);
    userService.assertAuthorized(token, userId);
    return ApiResponse.ok(userService.listRatings(userId));
  }

  @PutMapping("/ratings")
  public ApiResponse<UserRatingResponse> updateRating(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @Valid @RequestBody UserRatingUpdateRequest request) {
    String token = extractToken(authHeader);
    userService.assertAuthorized(token, request.getUserId());
    return ApiResponse.ok(userService.updateRating(request));
  }

  @DeleteMapping("/ratings/{id}")
  public ApiResponse<Boolean> deleteRating(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @PathVariable("id") Long id,
      @RequestParam("userId") @NotNull Long userId) {
    String token = extractToken(authHeader);
    userService.assertAuthorized(token, userId);
    userService.deleteRating(id, userId);
    return ApiResponse.ok(true);
  }
}
