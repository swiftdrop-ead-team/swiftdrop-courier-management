package com.swiftdrop.pricing.exception;

/**
 * Exception thrown when an explicitly provided promotional code fails validation,
 * such as being nonexistent, inactive, expired, ambiguous (multiple case-variant matches),
 * having an order amount below minimum requirements, or exceeding usage limits.
 */
public class InvalidPromoCodeException extends RuntimeException {

    public InvalidPromoCodeException(String message) {
        super(message);
    }

    public InvalidPromoCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
