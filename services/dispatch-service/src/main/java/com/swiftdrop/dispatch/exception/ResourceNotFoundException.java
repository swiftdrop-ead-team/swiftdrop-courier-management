package com.swiftdrop.dispatch.exception;

/**
 * Thrown when an entity (Courier, Assignment, or Proof of Delivery) cannot
 * be found in the database with the requested identifier (HTTP 404 Not Found).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
