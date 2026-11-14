package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.dto.*;
import com.swiftdrop.merchant.entity.MerchantApiKey;
import com.swiftdrop.merchant.entity.MerchantProfile;
import com.swiftdrop.merchant.exception.MerchantConflictException;
import com.swiftdrop.merchant.exception.MerchantForbiddenException;
import com.swiftdrop.merchant.exception.MerchantNotFoundException;
import com.swiftdrop.merchant.repository.MerchantApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MerchantApiKeyService {

    private static final Set<String> ALLOWED_SCOPES =
            Set.of("orders:read", "orders:write", "dashboard:read");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MerchantApiKeyRepository apiKeyRepository;
    private final MerchantProfileService profileService;

    @Transactional
    public MerchantApiKeyIssuedResponse issue(MerchantPrincipal principal, MerchantApiKeyRequest request) {
        ensureHumanPrincipal(principal);
        MerchantProfile profile = profileService.requireApproved(principal);
        if (request.getScopes() == null || request.getScopes().isEmpty()
                || !ALLOWED_SCOPES.containsAll(request.getScopes())) {
            throw new MerchantConflictException("One or more requested API key scopes are not supported");
        }

        byte[] randomKey = new byte[32];
        SECURE_RANDOM.nextBytes(randomKey);
        String rawKey = "sd_live_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomKey);
        String prefix = rawKey.substring(0, 16);
        String scopes = request.getScopes().stream().sorted().reduce((a, b) -> a + "," + b).orElseThrow();

        MerchantApiKey saved = apiKeyRepository.save(MerchantApiKey.builder()
                .merchantId(profile.getId())
                .keyPrefix(prefix)
                .keyHash(sha256(rawKey))
                .scopes(scopes)
                .build());
        return new MerchantApiKeyIssuedResponse(saved.getId(), rawKey, saved.getKeyPrefix(),
                saved.getScopes(), saved.getCreatedAt());
    }

    @Transactional
    public List<MerchantApiKeyResponse> list(MerchantPrincipal principal) {
        ensureHumanPrincipal(principal);
        MerchantProfile profile = profileService.requireApproved(principal);
        return apiKeyRepository.findByMerchantIdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .map(key -> new MerchantApiKeyResponse(key.getId(), key.getKeyPrefix(),
                        key.getScopes(), key.getCreatedAt(), key.getRevokedAt()))
                .toList();
    }

    @Transactional
    public void revoke(MerchantPrincipal principal, Long keyId) {
        ensureHumanPrincipal(principal);
        MerchantProfile profile = profileService.requireApproved(principal);
        MerchantApiKey apiKey = apiKeyRepository.findByIdAndMerchantId(keyId, profile.getId())
                .orElseThrow(() -> new MerchantNotFoundException("API key not found"));
        if (apiKey.getRevokedAt() == null) {
            apiKey.setRevokedAt(LocalDateTime.now());
            apiKeyRepository.save(apiKey);
        }
    }

    @Transactional
    public MerchantApiKeyIssuedResponse rotate(MerchantPrincipal principal, MerchantApiKeyRequest request) {
        ensureHumanPrincipal(principal);
        var profile=profileService.requireApprovedForOrder(principal);
        var issued=issue(principal,request);
        apiKeyRepository.findByMerchantIdOrderByCreatedAtDesc(profile.getId()).stream()
            .filter(key -> !key.getId().equals(issued.id()) && key.getRevokedAt()==null)
            .forEach(key -> {key.setRevokedAt(LocalDateTime.now());apiKeyRepository.save(key);});
        return issued;
    }

    private void ensureHumanPrincipal(MerchantPrincipal principal) {
        if (principal.apiKey()) {
            throw new MerchantForbiddenException("API keys cannot manage API keys");
        }
    }

    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
