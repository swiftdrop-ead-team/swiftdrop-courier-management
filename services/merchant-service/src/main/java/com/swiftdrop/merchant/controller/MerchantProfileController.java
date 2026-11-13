package com.swiftdrop.merchant.controller;

import com.swiftdrop.merchant.dto.*;
import com.swiftdrop.merchant.service.MerchantProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
@Tag(name = "Merchant Profiles", description = "Merchant registration, profile, and approval")
public class MerchantProfileController {

    private final MerchantProfileService profileService;

    @PostMapping("/profile")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Register a business profile for the authenticated Identity merchant")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> register(
            @AuthenticationPrincipal MerchantPrincipal principal,
            @Valid @RequestBody MerchantRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok("Merchant registration submitted for approval",
                        profileService.register(principal, request)));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Get the authenticated merchant's profile and approval state")
    public ApiResponse<MerchantProfileResponse> getMyProfile(
            @AuthenticationPrincipal MerchantPrincipal principal) {
        return ApiResponse.ok(profileService.getMyProfile(principal));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Update the authenticated merchant's business profile")
    public ApiResponse<MerchantProfileResponse> updateMyProfile(
            @AuthenticationPrincipal MerchantPrincipal principal,
            @Valid @RequestBody MerchantProfileUpdateRequest request) {
        return ApiResponse.ok("Merchant profile updated", profileService.updateMyProfile(principal, request));
    }

    @GetMapping("/admin/registrations/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "List merchant registrations awaiting approval")
    public ApiResponse<List<MerchantProfileResponse>> pendingMerchants() {
        return ApiResponse.ok(profileService.getPendingMerchants());
    }

    @PatchMapping("/admin/registrations/{merchantId}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Approve a pending merchant profile")
    public ApiResponse<MerchantProfileResponse> approve(
            @PathVariable UUID merchantId,
            @AuthenticationPrincipal MerchantPrincipal principal,
            @Valid @RequestBody(required = false) MerchantReviewRequest request) {
        return ApiResponse.ok("Merchant approved",
                profileService.approve(merchantId, principal.userId(), request == null ? null : request.getNotes()));
    }

    @PatchMapping("/admin/registrations/{merchantId}/reject")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Reject a pending merchant profile")
    public ApiResponse<MerchantProfileResponse> reject(
            @PathVariable UUID merchantId,
            @AuthenticationPrincipal MerchantPrincipal principal,
            @Valid @RequestBody(required = false) MerchantReviewRequest request) {
        return ApiResponse.ok("Merchant rejected",
                profileService.reject(merchantId, principal.userId(), request == null ? null : request.getNotes()));
    }
}
