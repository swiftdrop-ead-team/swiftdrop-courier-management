package com.swiftdrop.pricing.exception;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thrown when a price quote is found but its expiration timestamp has passed.
 * Distinct from {@link QuoteNotFoundException}: the quote exists but is no longer active.
 */
public class QuoteExpiredException extends RuntimeException {

    public QuoteExpiredException(UUID id, LocalDateTime expiresAt) {
        super("Price quote with ID " + id + " expired at " + expiresAt);
    }
}
