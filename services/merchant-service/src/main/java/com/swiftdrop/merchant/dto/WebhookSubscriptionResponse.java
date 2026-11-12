package com.swiftdrop.merchant.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WebhookSubscriptionResponse(
        Long id,
        String targetUrl,
        List<String> eventTypes,
        boolean active,
        String signingSecret,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
