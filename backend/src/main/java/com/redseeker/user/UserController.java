package com.redseeker.user;

import com.redseeker.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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

@RestController
@RequestMapping("/api/user")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  public ApiResponse<UserProfileResponse> register(@Valid @RequestBody UserRegisterRequest request) {
    return ApiResponse.ok(userService.register(request));
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
    return ApiResponse.ok(userService.login(request));
  }

  @GetMapping("/profile")
  public ApiResponse<UserProfileResponse> profile(
      @RequestHeader("X-Auth-Token") String token,
      @RequestParam("userId") @NotNull Long userId) {
    userService.assertAuthorized(token, userId);
    return ApiResponse.ok(userService.getProfile(userId));
  }

  @PutMapping("/profile")
  public ApiResponse<UserProfileResponse> updateProfile(
      @RequestHeader("X-Auth-Token") String token,
      @Valid @RequestBody UserProfileUpdateRequest request) {
    userService.assertAuthorized(token, request.getUserId());
    return ApiResponse.ok(userService.updateProfile(request));
  }

  @PostMapping("/browse")
  public ApiResponse<UserBrowseResponse> addBrowse(
      @RequestHeader("X-Auth-Token") String token,
      @Valid @RequestBody UserBrowseRequest request) {
    userService.assertAuthorized(token, request.getUserId());
    return ApiResponse.ok(userService.addBrowse(request));
  }

  @GetMapping("/browse")
  public ApiResponse<List<UserBrowseResponse>> listBrowse(
      @RequestHeader("X-Auth-Token") String token,
      @RequestParam("userId") @NotNull Long userId) {
    userService.assertAuthorized(token, userId);
    return ApiResponse.ok(userService.listBrowse(userId));
  }

  @PutMapping("/browse")
  public ApiResponse<UserBrowseResponse> updateBrowse(
      @RequestHeader("X-Auth-Token") String token,
      @Valid @RequestBody UserBrowseUpdateRequest request) {
    userService.assertAuthorized(token, request.getUserId());
    return ApiResponse.ok(userService.updateBrowse(request));
  }

  @DeleteMapping("/browse/{id}")
  public ApiResponse<Boolean> deleteBrowse(
      @RequestHeader("X-Auth-Token") String token,
      @PathVariable("id") Long id,
      @RequestParam("userId") @NotNull Long userId) {
    userService.assertAuthorized(token, userId);
    userService.deleteBrowse(id, userId);
    return ApiResponse.ok(true);
  }

  @PostMapping("/ratings")
  public ApiResponse<UserRatingResponse> addRating(
      @RequestHeader("X-Auth-Token") String token,
      @Valid @RequestBody UserRatingRequest request) {
    userService.assertAuthorized(token, request.getUserId());
    return ApiResponse.ok(userService.addRating(request));
  }

  @GetMapping("/ratings")
  public ApiResponse<List<UserRatingResponse>> listRatings(
      @RequestHeader("X-Auth-Token") String token,
      @RequestParam("userId") @NotNull Long userId) {
    userService.assertAuthorized(token, userId);
    return ApiResponse.ok(userService.listRatings(userId));
  }

  @PutMapping("/ratings")
  public ApiResponse<UserRatingResponse> updateRating(
      @RequestHeader("X-Auth-Token") String token,
      @Valid @RequestBody UserRatingUpdateRequest request) {
    userService.assertAuthorized(token, request.getUserId());
    return ApiResponse.ok(userService.updateRating(request));
  }

  @DeleteMapping("/ratings/{id}")
  public ApiResponse<Boolean> deleteRating(
      @RequestHeader("X-Auth-Token") String token,
      @PathVariable("id") Long id,
      @RequestParam("userId") @NotNull Long userId) {
    userService.assertAuthorized(token, userId);
    userService.deleteRating(id, userId);
    return ApiResponse.ok(true);
  }
}
