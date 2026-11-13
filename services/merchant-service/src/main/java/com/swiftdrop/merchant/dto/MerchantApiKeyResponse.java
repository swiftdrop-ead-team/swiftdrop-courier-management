package com.swiftdrop.merchant.dto;

import java.time.LocalDateTime;

public record MerchantApiKeyResponse(
        Long id,
        String keyPrefix,
        String scopes,
        LocalDateTime createdAt,
        LocalDateTime revokedAt) {
}
