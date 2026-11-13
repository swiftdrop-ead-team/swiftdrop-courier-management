package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.dto.*;
import com.swiftdrop.merchant.entity.MerchantOrder;
import com.swiftdrop.merchant.entity.MerchantProfile;
import com.swiftdrop.merchant.exception.MerchantForbiddenException;
import com.swiftdrop.merchant.exception.MerchantConflictException;
import com.swiftdrop.merchant.repository.MerchantOrderRepository;
import com.swiftdrop.merchant.repository.WebhookDeliveryRepository;
import com.swiftdrop.merchant.entity.WebhookDeliveryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantOrderService {

    private final MerchantOrderRepository orderRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final MerchantProfileService profileService;
    private final ParcelClient parcelClient;

    @Transactional
    public MerchantOrderResponse create(MerchantPrincipal principal, String idempotencyKey,
                                        MerchantOrderRequest request) {
        requireScope(principal, "orders:write");
        MerchantProfile profile = profileService.requireApprovedForOrder(principal);
        var existing = orderRepository.findByMerchantIdAndIdempotencyKey(profile.getId(), idempotencyKey);
        if (existing.isPresent()) {
            MerchantOrder order = existing.get();
            if (!matches(order, request)) {
                throw new MerchantConflictException("Idempotency-Key was already used with a different order");
            }
            return toResponse(order);
        }
        return createNew(profile, idempotencyKey, request);
    }

    @Transactional(readOnly = true)
    public List<MerchantOrderResponse> getOrders(MerchantPrincipal principal) {
        requireScope(principal, "orders:read");
        MerchantProfile profile = profileService.requireApproved(principal);
        return orderRepository.findByMerchantIdOrderByCreatedAtDesc(profile.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MerchantDashboardResponse dashboard(MerchantPrincipal principal) {
        requireScope(principal, "dashboard:read");
        MerchantProfile profile = profileService.requireApproved(principal);
        Map<String, Long> statuses = new LinkedHashMap<>();
        orderRepository.countStatusesByMerchantId(profile.getId())
                .forEach(row -> statuses.put((String) row[0], (Long) row[1]));
        return new MerchantDashboardResponse(orderRepository.countByMerchantId(profile.getId()), statuses,
                deliveryRepository.countByMerchantIdAndStatus(profile.getId(), WebhookDeliveryStatus.DELIVERED),
                deliveryRepository.countByMerchantIdAndStatus(profile.getId(), WebhookDeliveryStatus.FAILED));
    }

    private MerchantOrderResponse createNew(MerchantProfile profile, String key, MerchantOrderRequest request) {
        ParcelOrderRequest parcelRequest = new ParcelOrderRequest(key, request.getRecipientName().trim(),
                request.getRecipientPhone().trim(), request.getDeliveryAddress().trim(),
                request.getPickupAddress().trim(), trimToNull(request.getDescription()), request.getWeightKg());
        ParcelOrderResponse parcel = parcelClient.createParcel(key, parcelRequest);
        MerchantOrder order = MerchantOrder.builder()
                .merchantId(profile.getId())
                .idempotencyKey(key)
                .parcelId(parcel.parcelId())
                .trackingNumber(parcel.trackingNumber())
                .recipientName(parcelRequest.recipientName())
                .recipientPhone(parcelRequest.recipientPhone())
                .deliveryAddress(parcelRequest.deliveryAddress())
                .pickupAddress(parcelRequest.pickupAddress())
                .description(parcelRequest.description())
                .weightKg(parcelRequest.weightKg())
                .deliveryStatus("CREATED")
                .build();
        return toResponse(orderRepository.save(order));
    }

    private void requireScope(MerchantPrincipal principal, String scope) {
        if (principal.apiKey()) {
            var authentication = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            boolean allowed = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("SCOPE_" + scope));
            if (!allowed) {
                throw new MerchantForbiddenException("API key does not have the required scope");
            }
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean matches(MerchantOrder order, MerchantOrderRequest request) {
        return order.getRecipientName().equals(request.getRecipientName().trim())
                && order.getRecipientPhone().equals(request.getRecipientPhone().trim())
                && order.getDeliveryAddress().equals(request.getDeliveryAddress().trim())
                && order.getPickupAddress().equals(request.getPickupAddress().trim())
                && java.util.Objects.equals(order.getDescription(), trimToNull(request.getDescription()))
                && sameWeight(order.getWeightKg(), request.getWeightKg());
    }

    private boolean sameWeight(java.math.BigDecimal stored, java.math.BigDecimal requested) {
        return stored == null ? requested == null : requested != null && stored.compareTo(requested) == 0;
    }

    private MerchantOrderResponse toResponse(MerchantOrder order) {
        return new MerchantOrderResponse(order.getId(), order.getParcelId(), order.getTrackingNumber(),
                order.getRecipientName(), order.getRecipientPhone(), order.getDeliveryAddress(),
                order.getPickupAddress(), order.getDescription(), order.getWeightKg(),
                order.getDeliveryStatus(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
