package com.swiftdrop.merchant.controller;

import com.swiftdrop.merchant.dto.*;
import com.swiftdrop.merchant.service.MerchantApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchant/api-keys")
@RequiredArgsConstructor
@Tag(name = "Merchant API Keys", description = "Issue, list, and revoke scoped integration keys")
public class MerchantApiKeyController {

    private final MerchantApiKeyService apiKeyService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Issue a scoped API key; the complete key is returned once")
    public ApiResponse<MerchantApiKeyIssuedResponse> issue(
            @AuthenticationPrincipal MerchantPrincipal principal,
            @Valid @RequestBody MerchantApiKeyRequest request) {
        return ApiResponse.ok("API key issued; copy it now because it will not be shown again",
                apiKeyService.issue(principal, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "List API key prefixes and scopes without exposing key material")
    public ApiResponse<List<MerchantApiKeyResponse>> list(
            @AuthenticationPrincipal MerchantPrincipal principal) {
        return ApiResponse.ok(apiKeyService.list(principal));
    }

    @DeleteMapping("/{keyId}")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Revoke one of the authenticated merchant's API keys")
    public ApiResponse<Void> revoke(
            @AuthenticationPrincipal MerchantPrincipal principal, @PathVariable Long keyId) {
        apiKeyService.revoke(principal, keyId);
        return ApiResponse.ok("API key revoked", null);
    }
}
