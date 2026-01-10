package com.redseeker.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<ApiResponse<String>> handleServiceException(ServiceException ex) {
    ApiResponse<String> body = ApiResponse.fail(ex.getMessage(), ex.getErrorCode().getCode());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<String>> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getAllErrors().stream()
        .map(error -> error.getDefaultMessage())
        .findFirst()
        .orElse("Validation failed");
    ApiResponse<String> body = ApiResponse.fail(message, ErrorCode.VALIDATION_ERROR.getCode());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<String>> handleConstraintViolation(ConstraintViolationException ex) {
    ApiResponse<String> body = ApiResponse.fail(ex.getMessage(), ErrorCode.VALIDATION_ERROR.getCode());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<String>> handleGeneric(Exception ex) {
    ApiResponse<String> body = ApiResponse.fail(ex.getMessage(), ErrorCode.INTERNAL_ERROR.getCode());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
