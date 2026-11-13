package com.swiftdrop.dispatch.exception;

import com.swiftdrop.dispatch.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler for all REST controllers in the Dispatch Service.
 * Catches application exceptions and translates them into uniform JSON responses
 * with standard HTTP status codes (400, 404, 409, 500).
 * Prevents sensitive stack traces from leaking to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles entity not found errors (Returns HTTP 404 NOT FOUND).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles courier overload errors when active capacity limit is reached (Returns HTTP 409 CONFLICT).
     */
    @ExceptionHandler(CourierOverloadException.class)
    public ResponseEntity<ApiResponse<Void>> handleCourierOverload(CourierOverloadException ex) {
        log.warn("Courier overload conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles courier unavailable errors when courier is OFFLINE (Returns HTTP 400 BAD REQUEST).
     */
    @ExceptionHandler(CourierUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleCourierUnavailable(CourierUnavailableException ex) {
        log.warn("Courier unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles invalid assignment business logic errors (Returns HTTP 400 BAD REQUEST).
     */
    @ExceptionHandler(InvalidAssignmentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidAssignment(InvalidAssignmentException ex) {
        log.warn("Invalid assignment request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles Jakarta Bean Validation errors on DTOs (@NotBlank, @NotNull, etc.).
     * Returns HTTP 400 BAD REQUEST with field-by-field error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Validation failed for request: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    /**
     * Fallback handler for any uncaught system exceptions (Returns HTTP 500 INTERNAL SERVER ERROR).
     * Prevents internal details from being exposed to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Internal server error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
