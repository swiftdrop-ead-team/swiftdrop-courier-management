package com.swiftdrop.merchant.dto;

import java.util.Map;

public record MerchantDashboardResponse(
        long totalOrders,
        Map<String, Long> ordersByStatus,
        long successfulWebhookDeliveries,
        long failedWebhookDeliveries) {
}
