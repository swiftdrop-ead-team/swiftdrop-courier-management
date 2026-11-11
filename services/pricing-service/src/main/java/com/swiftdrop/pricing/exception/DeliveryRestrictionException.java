package com.swiftdrop.pricing.exception;

/**
 * Exception thrown when a shipment violates service delivery tier restrictions,
 * such as exceeding Express weight/dimension limits or Same-Day cutoff times.
 */
public class DeliveryRestrictionException extends RuntimeException {

    public DeliveryRestrictionException(String message) {
        super(message);
    }
}
