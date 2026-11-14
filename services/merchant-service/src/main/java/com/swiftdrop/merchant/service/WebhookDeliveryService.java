package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.entity.WebhookDelivery;
import com.swiftdrop.merchant.entity.WebhookDeliveryStatus;
import com.swiftdrop.merchant.entity.WebhookSubscription;
import com.swiftdrop.merchant.repository.WebhookDeliveryRepository;
import com.swiftdrop.merchant.repository.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookCryptoService cryptoService;
    private final WebhookUrlValidator urlValidator;
    private final HttpClient httpClient;

    @Value("${merchant.webhook.max-attempts:5}")
    private int maxAttempts;

    @Value("${merchant.webhook.base-delay-seconds:2}")
    private long baseDelaySeconds;

    @Value("${merchant.webhook.max-delay-seconds:300}")
    private long maxDelaySeconds;

    @Value("${merchant.webhook.request-timeout-seconds:10}")
    private long requestTimeoutSeconds;

    @Transactional
    public int processDueDeliveries() {
        List<WebhookDelivery> due = deliveryRepository
                .findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        WebhookDeliveryStatus.PENDING, LocalDateTime.now(), PageRequest.of(0, 100));
        for (WebhookDelivery delivery : due) {
            attempt(delivery);
        }
        return due.size();
    }

    @Transactional
    public void redeliver(WebhookDelivery delivery) {
        attempt(delivery);
    }

    private void attempt(WebhookDelivery delivery) {
        if (delivery.getStatus() != WebhookDeliveryStatus.PENDING) {
            return;
        }
        WebhookSubscription subscription = subscriptionRepository.findById(delivery.getWebhookId()).orElse(null);
        if (subscription == null || !subscription.isActive()) {
            fail(delivery, "Webhook subscription is inactive", null, true);
            return;
        }

        try {
            URI target = urlValidator.validate(subscription.getTargetUrl());
            String secret = cryptoService.decrypt(subscription.getSigningSecretEncrypted());
            String signature = cryptoService.sign(secret, delivery.getEventPayload());
            HttpRequest request = HttpRequest.newBuilder(target)
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("X-SwiftDrop-Event-Id", delivery.getEventKey())
                    .header("X-SwiftDrop-Signature", "sha256=" + signature)
                    .POST(HttpRequest.BodyPublishers.ofString(delivery.getEventPayload()))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                delivery.setStatus(WebhookDeliveryStatus.DELIVERED);
                delivery.setAttemptCount(delivery.getAttemptCount() + 1);
                delivery.setLastHttpStatus(response.statusCode());
                delivery.setLastError(null);
                delivery.setDeliveredAt(LocalDateTime.now());
                deliveryRepository.save(delivery);
                return;
            }
            fail(delivery, "Webhook receiver returned a non-success status", response.statusCode(), false);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fail(delivery, "Webhook delivery was interrupted", null, false);
        } catch (Exception ex) {
            fail(delivery, "Webhook delivery failed", null, false);
        }
    }

    private void fail(WebhookDelivery delivery, String message, Integer httpStatus, boolean permanent) {
        int attempts = delivery.getAttemptCount() + 1;
        delivery.setAttemptCount(attempts);
        delivery.setLastHttpStatus(httpStatus);
        delivery.setLastError(message);
        if (permanent || attempts >= maxAttempts) {
            delivery.setStatus(WebhookDeliveryStatus.FAILED);
        } else {
            delivery.setStatus(WebhookDeliveryStatus.PENDING);
            delivery.setNextAttemptAt(LocalDateTime.now().plusSeconds(retryDelay(attempts)));
        }
        deliveryRepository.save(delivery);
        log.warn("Webhook delivery {} failed on attempt {} ({})", delivery.getId(), attempts, message);
    }

    private long retryDelay(int attempt) {
        int exponent = Math.min(attempt - 1, 30);
        long multiplier = 1L << exponent;
        if (baseDelaySeconds > maxDelaySeconds / multiplier) {
            return maxDelaySeconds;
        }
        return Math.min(maxDelaySeconds, baseDelaySeconds * multiplier);
    }
}
