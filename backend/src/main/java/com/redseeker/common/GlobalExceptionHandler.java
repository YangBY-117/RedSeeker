package com.redseeker.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<ApiResponse<Object>> handleServiceException(ServiceException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    ApiResponse<Object> body = ApiResponse.fail(errorCode, ex.getMessage(), ex.getDetails());
    return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getAllErrors().stream()
        .map(error -> error.getDefaultMessage())
        .findFirst()
        .orElse(ErrorCode.VALIDATION_ERROR.getDefaultMessage());
    ApiResponse<Object> body = ApiResponse.fail(ErrorCode.VALIDATION_ERROR, message);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
    ApiResponse<Object> body = ApiResponse.fail(ErrorCode.VALIDATION_ERROR, ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Object>> handleUnreadable(HttpMessageNotReadableException ex) {
    ApiResponse<Object> body =
        ApiResponse.fail(ErrorCode.VALIDATION_ERROR, "Malformed request body");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
    ApiResponse<Object> body = ApiResponse.fail(ErrorCode.VALIDATION_ERROR, ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
    LOGGER.error("Unhandled exception", ex);
    ApiResponse<Object> body =
        ApiResponse.fail(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
