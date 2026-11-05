package com.swiftdrop.notification.entity;

/**
 * Lifecycle status of a notification dispatch attempt.
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED_DUPLICATE
}

