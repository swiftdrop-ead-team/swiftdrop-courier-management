package com.swiftdrop.pricing.exception;

/**
 * Exception thrown when no active fuel surcharge record is effective for the calculation date.
 */
public class FuelRateNotFoundException extends RuntimeException {

    public FuelRateNotFoundException(String message) {
        super(message);
    }
}
