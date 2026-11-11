package com.swiftdrop.pricing.exception;

/**
 * Exception thrown when an invalid, null, or unsupported delivery service tier
 * is requested from the pricing strategy registry.
 */
public class UnsupportedPricingTierException extends RuntimeException {

    public UnsupportedPricingTierException(String message) {
        super(message);
    }
}
