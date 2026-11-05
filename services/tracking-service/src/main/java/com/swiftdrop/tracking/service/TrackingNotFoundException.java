package com.swiftdrop.tracking.service;

public class TrackingNotFoundException extends RuntimeException {
    public TrackingNotFoundException(String trackingNumber) {
        super("No tracking record found for tracking number: " + trackingNumber);
    }
}
