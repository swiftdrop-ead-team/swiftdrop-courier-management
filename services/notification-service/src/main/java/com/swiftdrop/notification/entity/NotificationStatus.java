package com.swiftdrop.notification.entity;

/**
 * Lifecycle status of a notification dispatch attempt.
 */
public enum NotificationStatus {
    SKIPPED_PREFERENCE,
    PENDING,
    SENT,
    FAILED,
    SKIPPED_DUPLICATE
}

