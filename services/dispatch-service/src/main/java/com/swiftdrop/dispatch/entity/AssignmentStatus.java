package com.swiftdrop.dispatch.entity;

/**
 * State lifecycle of a parcel dispatch assignment.
 */
public enum AssignmentStatus {
    // Parcel has been assigned to a courier by the dispatcher
    ASSIGNED,

    // Courier has physically collected the parcel from pickup address / hub
    PICKED_UP,

    // Parcel is moving between logistics hubs or transport facilities
    IN_TRANSIT,

    // Courier is on the way to the recipient's delivery location
    OUT_FOR_DELIVERY,

    // Parcel has been successfully handed over and Proof of Delivery is recorded
    DELIVERED,

    // Delivery attempt failed (e.g., recipient unreachable or address not found)
    FAILED,

    // Assignment was cancelled before completion
    CANCELLED
}
