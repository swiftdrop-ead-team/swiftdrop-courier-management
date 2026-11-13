package com.swiftdrop.pricing.exception;

import com.swiftdrop.pricing.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized REST exception handler advising all pricing controllers.
 * Translates domain, validation, and system exceptions into standardized {@link ApiResponse} payloads.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles entities not found (404 Not Found).
     */
    @ExceptionHandler({QuoteNotFoundException.class, ZoneNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(RuntimeException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles expired price quotes (410 Gone).
     */
    @ExceptionHandler(QuoteExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleQuoteExpired(QuoteExpiredException ex) {
        log.info("Quote expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles client business rule violations (400 Bad Request).
     */
    @ExceptionHandler({
            InactiveZoneException.class,
            InvalidPromoCodeException.class,
            DeliveryRestrictionException.class,
            UnsupportedPricingTierException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleClientBusinessError(RuntimeException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles Bean Validation constraint failures (400 Bad Request).
     * Returns field-level violation messages in the data field.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Payload validation failed with {} error(s)", errors.size());

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed")
                .data(errors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles request parameter or path variable type conversion errors (400 Bad Request).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        log.warn("Type mismatch: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * Handles malformed JSON payloads or unreadable HTTP messages (400 Bad Request).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed HTTP message: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Malformed request payload or unreadable JSON format"));
    }

    /**
     * Handles missing fuel rate configuration (500 Internal Server Error).
     * Returns safe message to client while logging detailed error internally.
     */
    @ExceptionHandler(FuelRateNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleFuelRateNotFound(FuelRateNotFoundException ex) {
        log.error("Fuel surcharge rate configuration missing: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Current fuel surcharge rate is not configured. Please contact support."));
    }

    /**
     * Catch-all handler for unexpected application exceptions (500 Internal Server Error).
     * Conceals internal stack traces and implementation details from clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Unhandled server error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected internal error occurred. Please try again later."));
    }
}
