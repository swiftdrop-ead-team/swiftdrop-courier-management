package com.swiftdrop.merchant.controller;

import com.swiftdrop.merchant.dto.*;
import com.swiftdrop.merchant.service.MerchantOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchant/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Merchant Orders", description = "Versioned merchant order intake and history")
public class MerchantOrderController {

    private final MerchantOrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_MERCHANT') and hasAuthority('SCOPE_orders:write')")
    @Operation(summary = "Create an order with duplicate-request protection")
    public ApiResponse<MerchantOrderResponse> create(
            @AuthenticationPrincipal MerchantPrincipal principal,
            @RequestHeader("Idempotency-Key")
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "^[A-Za-z0-9._:-]{1,100}$") String idempotencyKey,
            @Valid @RequestBody MerchantOrderRequest request) {
        return ApiResponse.ok("Order accepted",
                orderService.create(principal, idempotencyKey, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_MERCHANT') and hasAuthority('SCOPE_orders:read')")
    @Operation(summary = "List only the authenticated merchant's orders and delivery states")
    public ApiResponse<List<MerchantOrderResponse>> getOrders(
            @AuthenticationPrincipal MerchantPrincipal principal) {
        return ApiResponse.ok(orderService.getOrders(principal));
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT') and hasAuthority('SCOPE_dashboard:read')")
    @Operation(summary = "Get owned order status counts and webhook delivery totals")
    public ApiResponse<MerchantDashboardResponse> getDashboard(
            @AuthenticationPrincipal MerchantPrincipal principal) {
        return ApiResponse.ok(orderService.dashboard(principal));
    }
}
