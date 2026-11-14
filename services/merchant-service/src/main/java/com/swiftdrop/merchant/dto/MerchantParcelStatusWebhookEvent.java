package com.swiftdrop.merchant.dto;

import java.time.LocalDateTime;

public record MerchantParcelStatusWebhookEvent(
        String eventId,
        String eventType,
        String parcelId,
        String trackingNumber,
        String previousStatus,
        String status,
        String failureReason,
        LocalDateTime timestamp,
        Long version) {
}
