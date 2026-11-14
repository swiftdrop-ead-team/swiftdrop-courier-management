package com.swiftdrop.merchant.dto;

import java.time.LocalDateTime;

public record MerchantApiKeyIssuedResponse(
        Long id,
        String key,
        String keyPrefix,
        String scopes,
        LocalDateTime createdAt) {
}
