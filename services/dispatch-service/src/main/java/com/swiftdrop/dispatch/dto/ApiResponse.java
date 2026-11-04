package com.swiftdrop.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard uniform API response wrapper for all dispatch REST endpoints.
 * Ensures consistent response format: success flag, message, data, and timestamp.
 *
 * @param <T> The payload object type returned in the response data field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    // Indicates whether the request succeeded (true) or failed (false)
    @Builder.Default
    private boolean success = true;

    // Optional human-readable message describing the outcome
    private String message;

    // Actual payload data returned to the caller
    private T data;

    // Timestamp when the response was generated
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Utility method to create a successful response with data.
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * Utility method to create a successful response with message and data.
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Utility method to create an error response.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
