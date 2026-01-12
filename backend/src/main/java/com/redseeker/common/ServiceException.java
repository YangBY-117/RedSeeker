package com.redseeker.common;

public class ServiceException extends RuntimeException {
  private final ErrorCode errorCode;
  private final Object details;

  public ServiceException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
    this.details = null;
  }

  public ServiceException(ErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
    this.details = null;
  }

  public ServiceException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.details = null;
  }

  public ServiceException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getDefaultMessage(), cause);
    this.errorCode = errorCode;
    this.details = null;
  }

  public ServiceException(ErrorCode errorCode, String message, Object details) {
    super(message);
    this.errorCode = errorCode;
    this.details = details;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public Object getDetails() {
    return details;
  }
}
