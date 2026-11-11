package com.swiftdrop.pricing.exception;

/**
 * Exception thrown when a requested delivery zone code is not found in the database.
 */
public class ZoneNotFoundException extends RuntimeException {

    public ZoneNotFoundException(String message) {
        super(message);
    }
}
