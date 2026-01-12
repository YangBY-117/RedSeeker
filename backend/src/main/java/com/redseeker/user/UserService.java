package com.redseeker.user;

import java.util.List;

public interface UserService {
  UserProfileResponse register(UserRegisterRequest request);

  LoginResponse login(UserLoginRequest request);

  UserProfileResponse getProfile(Long userId);

  UserProfileResponse updateProfile(UserProfileUpdateRequest request);

  UserBrowseResponse addBrowse(UserBrowseRequest request);

  List<UserBrowseResponse> listBrowse(Long userId);

  UserBrowseResponse updateBrowse(UserBrowseUpdateRequest request);

  void deleteBrowse(Long id, Long userId);

  UserRatingResponse addRating(UserRatingRequest request);

  List<UserRatingResponse> listRatings(Long userId);

  UserRatingResponse updateRating(UserRatingUpdateRequest request);

  void deleteRating(Long id, Long userId);

  void assertAuthorized(String token, Long userId);
}
