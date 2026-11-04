package com.swiftdrop.dispatch.exception;

/**
 * Thrown when a dispatcher attempts to assign a parcel to a courier who has
 * already reached their maximum active delivery capacity (HTTP 409 Conflict).
 */
public class CourierOverloadException extends RuntimeException {
    public CourierOverloadException(String message) {
        super(message);
    }
}
