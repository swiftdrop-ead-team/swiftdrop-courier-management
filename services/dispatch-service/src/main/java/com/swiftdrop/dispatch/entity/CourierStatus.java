package com.swiftdrop.dispatch.entity;

/**
 * Represents the current operational availability status of a courier.
 */
public enum CourierStatus {
    // Courier is ready to take new delivery assignments
    AVAILABLE,

    // Courier has reached their maximum delivery workload limit
    ASSIGNED,

    // Courier is actively on the road delivering parcels
    DELIVERING,

    // Courier is off-duty and cannot receive any delivery assignments
    OFFLINE
}
