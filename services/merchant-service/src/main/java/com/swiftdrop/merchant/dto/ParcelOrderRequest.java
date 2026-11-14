package com.swiftdrop.merchant.dto;

import java.math.BigDecimal;

public record ParcelOrderRequest(
        String merchantOrderReference,
        String recipientName,
        String recipientPhone,
        String deliveryAddress,
        String pickupAddress,
        String description,
        BigDecimal weightKg) {
}
