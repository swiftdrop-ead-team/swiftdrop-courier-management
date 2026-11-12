package com.swiftdrop.merchant.dto;

import java.time.LocalDateTime;

public record WebhookDeliveryResponse(
        Long id,
        Long webhookId,
        String eventKey,
        String status,
        int attemptCount,
        LocalDateTime nextAttemptAt,
        Integer lastHttpStatus,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime deliveredAt) {
}
