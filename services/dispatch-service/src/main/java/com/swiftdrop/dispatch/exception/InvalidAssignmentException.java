package com.swiftdrop.dispatch.exception;

/**
 * Thrown when an assignment request violates business rules, such as assigning
 * a parcel that is already actively assigned to another courier (HTTP 400 Bad Request).
 */
public class InvalidAssignmentException extends RuntimeException {
    public InvalidAssignmentException(String message) {
        super(message);
    }
}
