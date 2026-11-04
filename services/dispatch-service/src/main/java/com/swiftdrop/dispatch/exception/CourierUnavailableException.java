package com.swiftdrop.dispatch.exception;

/**
 * Thrown when attempting an operation on a courier who is currently OFFLINE
 * or unavailable for assignments (HTTP 400 Bad Request).
 */
public class CourierUnavailableException extends RuntimeException {
    public CourierUnavailableException(String message) {
        super(message);
    }
}
