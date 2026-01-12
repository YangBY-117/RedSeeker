package com.redseeker.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
  private final boolean success;
  private final String message;
  private final T data;
  private final String code;
  private final Instant timestamp;

  private ApiResponse(boolean success, String message, T data, String code) {
    this.success = success;
    this.message = message;
    this.data = data;
    this.code = code;
    this.timestamp = Instant.now();
  }

  public static <T> ApiResponse<T> ok(String message, T data) {
    return new ApiResponse<>(true, message, data, null);
  }

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, "OK", data, null);
  }

  public static <T> ApiResponse<T> fail(String message, T data) {
    return new ApiResponse<>(false, message, data, null);
  }

  public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
    return new ApiResponse<>(false, errorCode.getDefaultMessage(), null, errorCode.getCode());
  }

  public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
    return new ApiResponse<>(false, message, null, errorCode.getCode());
  }

  public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message, T data) {
    return new ApiResponse<>(false, message, data, errorCode.getCode());
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }

  public String getCode() {
    return code;
  }

  public Instant getTimestamp() {
    return timestamp;
  }
}
