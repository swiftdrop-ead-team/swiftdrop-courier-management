package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.dto.*;
import com.swiftdrop.merchant.entity.*;
import com.swiftdrop.merchant.exception.MerchantConflictException;
import com.swiftdrop.merchant.exception.MerchantForbiddenException;
import com.swiftdrop.merchant.exception.MerchantNotFoundException;
import com.swiftdrop.merchant.repository.WebhookDeliveryRepository;
import com.swiftdrop.merchant.repository.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookSubscriptionService {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(MerchantParcelEventService.WEBHOOK_EVENT_TYPE);

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final MerchantProfileService profileService;
    private final WebhookCryptoService cryptoService;
    private final WebhookUrlValidator urlValidator;
    private final WebhookDeliveryService deliveryService;

    @Transactional
    public WebhookSubscriptionResponse create(
            MerchantPrincipal principal, WebhookSubscriptionRequest request) {
        MerchantProfile profile = requireHumanApprovedMerchant(principal);
        String targetUrl = validateRequest(request);
        String secret = cryptoService.generateSecret();
        WebhookSubscription subscription = subscriptionRepository.save(WebhookSubscription.builder()
                .merchantId(profile.getId())
                .targetUrl(targetUrl)
                .eventTypes(joinEvents(request.getEventTypes()))
                .signingSecretEncrypted(cryptoService.encrypt(secret))
                .active(true)
                .build());
        return toResponse(subscription, secret);
    }

    @Transactional
    public List<WebhookSubscriptionResponse> list(MerchantPrincipal principal) {
        MerchantProfile profile = requireHumanApprovedMerchant(principal);
        return subscriptionRepository.findByMerchantIdOrderByCreatedAtDesc(profile.getId())
                .stream().map(subscription -> toResponse(subscription, null)).toList();
    }

    @Transactional
    public WebhookSubscriptionResponse update(
            MerchantPrincipal principal, Long webhookId, WebhookSubscriptionRequest request) {
        MerchantProfile profile = requireHumanApprovedMerchant(principal);
        WebhookSubscription subscription = getOwnedSubscription(webhookId, profile.getId());
        subscription.setTargetUrl(validateRequest(request));
        subscription.setEventTypes(joinEvents(request.getEventTypes()));
        String secret = cryptoService.generateSecret();
        subscription.setSigningSecretEncrypted(cryptoService.encrypt(secret));
        subscription.setActive(true);
        return toResponse(subscriptionRepository.save(subscription), secret);
    }

    @Transactional
    public void delete(MerchantPrincipal principal, Long webhookId) {
        MerchantProfile profile = requireHumanApprovedMerchant(principal);
        WebhookSubscription subscription = getOwnedSubscription(webhookId, profile.getId());
        subscription.setActive(false);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public List<WebhookDeliveryResponse> deliveries(MerchantPrincipal principal, Long webhookId) {
        MerchantProfile profile = requireHumanApprovedMerchant(principal);
        WebhookSubscription subscription = getOwnedSubscription(webhookId, profile.getId());
        return deliveryRepository.findByWebhookIdOrderByCreatedAtDesc(subscription.getId())
                .stream().map(this::toDeliveryResponse).toList();
    }

    @Transactional
    public WebhookDeliveryResponse redeliver(
            MerchantPrincipal principal, Long webhookId, Long deliveryId) {
        MerchantProfile profile = requireHumanApprovedMerchant(principal);
        WebhookSubscription subscription = getOwnedSubscription(webhookId, profile.getId());
        WebhookDelivery delivery = deliveryRepository.findByIdAndWebhookId(deliveryId, subscription.getId())
                .orElseThrow(() -> new MerchantNotFoundException("Webhook delivery not found"));
        if (delivery.getStatus() != WebhookDeliveryStatus.FAILED) {
            throw new MerchantConflictException("Only failed webhook deliveries can be redelivered");
        }
        delivery.setStatus(WebhookDeliveryStatus.PENDING);
        delivery.setAttemptCount(0);
        delivery.setNextAttemptAt(LocalDateTime.now());
        delivery.setLastError(null);
        deliveryRepository.save(delivery);
        deliveryService.redeliver(delivery);
        return toDeliveryResponse(delivery);
    }

    private MerchantProfile requireHumanApprovedMerchant(MerchantPrincipal principal) {
        if (principal.apiKey()) {
            throw new MerchantForbiddenException("Webhook management requires a Merchant JWT");
        }
        return profileService.requireApproved(principal);
    }

    private String validateRequest(WebhookSubscriptionRequest request) {
        if (request.getEventTypes() == null || request.getEventTypes().isEmpty()
                || !SUPPORTED_EVENTS.containsAll(request.getEventTypes())) {
            throw new MerchantConflictException("Only parcel.status.changed webhook events are supported");
        }
        String url = request.getTargetUrl().trim();
        urlValidator.validate(url);
        return url;
    }

    private String joinEvents(Set<String> eventTypes) {
        return eventTypes.stream().sorted().reduce((a, b) -> a + "," + b).orElseThrow();
    }

    private WebhookSubscription getOwnedSubscription(Long id, UUID merchantId) {
        return subscriptionRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new MerchantNotFoundException("Webhook subscription not found"));
    }

    private WebhookSubscriptionResponse toResponse(WebhookSubscription subscription, String oneTimeSecret) {
        return new WebhookSubscriptionResponse(subscription.getId(), subscription.getTargetUrl(),
                Arrays.stream(subscription.getEventTypes().split(",")).toList(),
                subscription.isActive(), oneTimeSecret, subscription.getCreatedAt(), subscription.getUpdatedAt());
    }

    private String payloadField(WebhookDelivery delivery,String field) {
        try {return new com.fasterxml.jackson.databind.ObjectMapper().readTree(delivery.getEventPayload()).path(field).asText("");}
        catch(Exception e){return "";}
    }

    private WebhookDeliveryResponse toDeliveryResponse(WebhookDelivery delivery) {
        return new WebhookDeliveryResponse(delivery.getId(), delivery.getWebhookId(), delivery.getEventKey(),
                delivery.getStatus().name(), delivery.getAttemptCount(), delivery.getNextAttemptAt(),
                delivery.getLastHttpStatus(), delivery.getLastError(), delivery.getCreatedAt(),
                delivery.getDeliveredAt(), payloadField(delivery,"trackingNumber"),payloadField(delivery,"status"));
    }
}
