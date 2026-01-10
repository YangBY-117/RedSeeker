package com.redseeker.common;

public enum ErrorCode {
  VALIDATION_ERROR("VALIDATION_ERROR"),
  NOT_FOUND("NOT_FOUND"),
  INTERNAL_ERROR("INTERNAL_ERROR");

  private final String code;

  ErrorCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
