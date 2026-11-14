package com.swiftdrop.merchant.dto;

import java.util.UUID;

public record MerchantPrincipal(UUID userId, String email, boolean apiKey) {
}
