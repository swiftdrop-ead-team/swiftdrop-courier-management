package com.swiftdrop.merchant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.merchant.dto.DeliveryStatusUpdatedEvent;
import com.swiftdrop.merchant.dto.MerchantParcelStatusWebhookEvent;
import com.swiftdrop.merchant.entity.*;
import com.swiftdrop.merchant.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantParcelEventService {

    public static final String WEBHOOK_EVENT_TYPE = "parcel.status.changed";

    private final MerchantOrderRepository orderRepository;
    private final ProcessedParcelEventRepository processedEventRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(DeliveryStatusUpdatedEvent event, String messageId) {
        if (event == null || event.getNewStatus() == null || event.getNewStatus().isBlank()
                || ((event.getParcelId() == null || event.getParcelId().isBlank())
                && (event.getTrackingNumber() == null || event.getTrackingNumber().isBlank()))) {
            throw new IllegalArgumentException("Parcel status event is missing required identifiers or status");
        }

        String eventKey = eventKey(event, messageId);
        if (processedEventRepository.existsById(eventKey)) {
            return;
        }

        LocalDateTime occurredAt = event.getTimestamp() == null ? LocalDateTime.now() : event.getTimestamp();
        String eventType = event.getEventType() == null ? "DELIVERY_STATUS_UPDATED" : event.getEventType();
        processedEventRepository.save(ProcessedParcelEvent.builder()
                .eventKey(eventKey)
                .parcelId(event.getParcelId() == null ? event.getTrackingNumber() : event.getParcelId())
                .eventType(eventType)
                .occurredAt(occurredAt)
                .build());

        MerchantOrder order = orderRepository
                .findFirstByParcelIdOrTrackingNumber(event.getParcelId(), event.getTrackingNumber())
                .orElse(null);
        if (order == null || !isNewer(event, order, occurredAt)) {
            return;
        }

        order.setDeliveryStatus(event.getNewStatus());
        order.setParcelStatusUpdatedAt(occurredAt);
        if (event.getVersion() != null) {
            order.setStatusVersion(event.getVersion());
        }
        orderRepository.save(order);

        MerchantParcelStatusWebhookEvent webhookEvent = new MerchantParcelStatusWebhookEvent(
                eventKey, WEBHOOK_EVENT_TYPE, event.getParcelId(), event.getTrackingNumber(),
                event.getPreviousStatus(), event.getNewStatus(), event.getFailureReason(),
                occurredAt, event.getVersion());
        String payload = serialize(webhookEvent);
        List<WebhookSubscription> subscriptions = subscriptionRepository
                .findByMerchantIdAndActiveTrue(order.getMerchantId());
        for (WebhookSubscription subscription : subscriptions) {
            if (subscription.getEventTypes().contains(WEBHOOK_EVENT_TYPE)) {
                deliveryRepository.save(WebhookDelivery.builder()
                        .webhookId(subscription.getId())
                        .eventKey(eventKey)
                        .orderId(order.getId())
                        .eventPayload(payload)
                        .status(WebhookDeliveryStatus.PENDING)
                        .attemptCount(0)
                        .nextAttemptAt(LocalDateTime.now())
                        .build());
            }
        }
    }

    private boolean isNewer(DeliveryStatusUpdatedEvent event, MerchantOrder order, LocalDateTime occurredAt) {
        if (event.getVersion() != null) {
            return order.getStatusVersion() == null || event.getVersion() > order.getStatusVersion();
        }
        return order.getParcelStatusUpdatedAt() == null || occurredAt.isAfter(order.getParcelStatusUpdatedAt());
    }

    private String serialize(MerchantParcelStatusWebhookEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize parcel status webhook event", ex);
        }
    }

    private String eventKey(DeliveryStatusUpdatedEvent event, String messageId) {
        String value;
        if (messageId != null && !messageId.isBlank()) {
            value = "message:" + messageId;
        } else if (event.getEventId() != null && !event.getEventId().isBlank()) {
            value = "event:" + event.getEventId();
        } else {
            value = String.join("|",
                    nullToEmpty(event.getParcelId()),
                    nullToEmpty(event.getTrackingNumber()),
                    nullToEmpty(event.getPreviousStatus()),
                    nullToEmpty(event.getNewStatus()),
                    event.getTimestamp() == null ? "" : event.getTimestamp().toString(),
                    event.getVersion() == null ? "" : event.getVersion().toString());
        }
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
