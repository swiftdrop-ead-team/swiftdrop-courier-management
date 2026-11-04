package com.swiftdrop.dispatch.entity;

/**
 * Type of transport vehicle used by the courier.
 * Determines capacity and suitable delivery routes.
 */
public enum VehicleType {
    // Motorcycle for small, fast parcels
    MOTORBIKE,

    // Three-wheeler / Auto-rickshaw for medium deliveries
    THREE_WHEELER,

    // Van for larger parcel volumes
    VAN,

    // Heavy truck for commercial or bulk freight
    TRUCK
}
