package com.swiftdrop.pricing.exception;

import java.util.UUID;

/**
 * Thrown when no price quote is found for the given identifier.
 */
public class QuoteNotFoundException extends RuntimeException {

    public QuoteNotFoundException(UUID id) {
        super("Price quote not found with ID: " + id);
    }
}
