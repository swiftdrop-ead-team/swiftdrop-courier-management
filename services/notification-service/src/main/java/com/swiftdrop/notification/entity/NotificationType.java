package com.swiftdrop.notification.entity;

/**
 * Domain milestone event that triggers a customer/courier notification.
 */
public enum NotificationType {
    ORDER_CONFIRMED,
    COURIER_ASSIGNED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELIVERY_FAILED
}

