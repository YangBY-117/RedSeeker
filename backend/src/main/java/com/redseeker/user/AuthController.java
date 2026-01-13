package com.redseeker.user;

import com.redseeker.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
  private final UserService userService;

  public AuthController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  public ApiResponse<LoginResponse> register(@Valid @RequestBody UserRegisterRequest request) {
    // Register user and auto login, return token and user info
    userService.register(request);
    // Auto login after registration
    UserLoginRequest loginRequest = new UserLoginRequest();
    loginRequest.setUsername(request.getUsername());
    loginRequest.setPassword(request.getPassword());
    LoginResponse loginResponse = userService.login(loginRequest);
    return ApiResponse.ok(loginResponse);
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
    LoginResponse loginResponse = userService.login(request);
    return ApiResponse.ok(loginResponse);
  }

  @GetMapping("/me")
  public ApiResponse<UserProfileResponse> getCurrentUser(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    // Extract token from Authorization header
    // Format: Bearer {token}
    String token = null;
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      token = authHeader.substring(7);
    }
    
    if (token == null || token.isBlank()) {
      throw new com.redseeker.common.ServiceException(
          com.redseeker.common.ErrorCode.UNAUTHORIZED, 
          "Authorization token is required"
      );
    }
    
    // Get user ID from token
    Long userId = AuthTokenStore.resolveUserId(token);
    if (userId == null) {
      throw new com.redseeker.common.ServiceException(
          com.redseeker.common.ErrorCode.UNAUTHORIZED, 
          "Invalid or expired token"
      );
    }
    
    // Get user profile
    UserProfileResponse profile = userService.getProfile(userId);
    return ApiResponse.ok(profile);
  }
}
