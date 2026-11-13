package com.swiftdrop.merchant.controller;

import com.swiftdrop.merchant.dto.*;
import com.swiftdrop.merchant.service.WebhookSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchant/webhooks")
@RequiredArgsConstructor
@Tag(name = "Merchant Webhooks", description = "Signed webhook subscriptions and delivery audit")
public class WebhookSubscriptionController {

    private final WebhookSubscriptionService subscriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Create an HTTPS webhook subscription; signing secret is shown once")
    public ApiResponse<WebhookSubscriptionResponse> create(
            @AuthenticationPrincipal MerchantPrincipal principal,
            @Valid @RequestBody WebhookSubscriptionRequest request) {
        return ApiResponse.ok("Webhook subscription created; signing secret is shown once",
                subscriptionService.create(principal, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "List only the authenticated merchant's webhook subscriptions")
    public ApiResponse<List<WebhookSubscriptionResponse>> list(
            @AuthenticationPrincipal MerchantPrincipal principal) {
        return ApiResponse.ok(subscriptionService.list(principal));
    }

    @PutMapping("/{webhookId}")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Update a webhook and rotate its signing secret")
    public ApiResponse<WebhookSubscriptionResponse> update(
            @AuthenticationPrincipal MerchantPrincipal principal,
            @PathVariable Long webhookId,
            @Valid @RequestBody WebhookSubscriptionRequest request) {
        return ApiResponse.ok("Webhook subscription updated; new signing secret is shown once",
                subscriptionService.update(principal, webhookId, request));
    }

    @DeleteMapping("/{webhookId}")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Disable a webhook subscription")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal MerchantPrincipal principal, @PathVariable Long webhookId) {
        subscriptionService.delete(principal, webhookId);
        return ApiResponse.ok("Webhook subscription disabled", null);
    }

    @GetMapping("/{webhookId}/deliveries")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "View delivery attempts for one of the merchant's webhooks")
    public ApiResponse<List<WebhookDeliveryResponse>> deliveries(
            @AuthenticationPrincipal MerchantPrincipal principal, @PathVariable Long webhookId) {
        return ApiResponse.ok(subscriptionService.deliveries(principal, webhookId));
    }

    @PostMapping("/{webhookId}/deliveries/{deliveryId}/redeliver")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Manually retry a failed webhook delivery")
    public ApiResponse<WebhookDeliveryResponse> redeliver(
            @AuthenticationPrincipal MerchantPrincipal principal,
            @PathVariable Long webhookId, @PathVariable Long deliveryId) {
        return ApiResponse.ok("Webhook delivery retried",
                subscriptionService.redeliver(principal, webhookId, deliveryId));
    }
}
