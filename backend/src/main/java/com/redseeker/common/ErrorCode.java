package com.redseeker.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Validation failed"),
  NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND, "Resource not found"),
  UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Unauthorized"),
  INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;

  ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
    this.code = code;
    this.httpStatus = httpStatus;
    this.defaultMessage = defaultMessage;
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }
}
