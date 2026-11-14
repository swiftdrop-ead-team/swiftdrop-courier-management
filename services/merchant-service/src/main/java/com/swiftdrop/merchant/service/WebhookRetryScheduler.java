package com.swiftdrop.merchant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "merchant.webhook.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class WebhookRetryScheduler {

    private final WebhookDeliveryService deliveryService;

    @Scheduled(fixedDelayString = "${WEBHOOK_RETRY_SCAN_MS:5000}")
    public void deliverDueWebhooks() {
        deliveryService.processDueDeliveries();
    }
}
