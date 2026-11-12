package com.swiftdrop.merchant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MerchantOrderResponse(
        Long id,
        String parcelId,
        String trackingNumber,
        String recipientName,
        String recipientPhone,
        String deliveryAddress,
        String pickupAddress,
        String description,
        BigDecimal weightKg,
        String deliveryStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
