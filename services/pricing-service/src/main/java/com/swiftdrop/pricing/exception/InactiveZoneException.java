package com.swiftdrop.pricing.exception;

/**
 * Exception thrown when a delivery zone exists in the database but is marked inactive.
 */
public class InactiveZoneException extends RuntimeException {

    public InactiveZoneException(String message) {
        super(message);
    }
}
