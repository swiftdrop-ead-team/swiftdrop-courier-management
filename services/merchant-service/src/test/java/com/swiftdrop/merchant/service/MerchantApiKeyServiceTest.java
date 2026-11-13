package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.dto.MerchantApiKeyRequest;
import com.swiftdrop.merchant.dto.MerchantPrincipal;
import com.swiftdrop.merchant.entity.MerchantApiKey;
import com.swiftdrop.merchant.entity.MerchantProfile;
import com.swiftdrop.merchant.exception.MerchantForbiddenException;
import com.swiftdrop.merchant.repository.MerchantApiKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantApiKeyServiceTest {

    @Mock
    private MerchantApiKeyRepository apiKeyRepository;
    @Mock
    private MerchantProfileService profileService;
    @InjectMocks
    private MerchantApiKeyService service;

    @Test
    void issueStoresOnlyHashAndReturnsRawKeyOnce() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        when(profileService.requireApproved(any())).thenReturn(MerchantProfile.builder().id(merchantId).build());
        when(apiKeyRepository.save(any(MerchantApiKey.class))).thenAnswer(call -> {
            MerchantApiKey key = call.getArgument(0);
            key.setId(3L);
            return key;
        });

        var issued = service.issue(new MerchantPrincipal(userId, "m@example.test", false),
                MerchantApiKeyRequest.builder().scopes(Set.of("orders:read")).build());
        String expectedHash = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(issued.key().getBytes(StandardCharsets.UTF_8)));

        verify(apiKeyRepository).save(argThat(key -> key.getKeyHash().equals(expectedHash)
                && !key.getKeyHash().equals(issued.key())
                && key.getKeyPrefix().equals(issued.keyPrefix())
                && key.getMerchantId().equals(merchantId)));
        assertThat(issued.key()).startsWith("sd_live_");
        assertThat(issued.key()).isNotBlank();
    }

    @Test
    void apiKeyCannotManageOtherApiKeys() {
        MerchantPrincipal apiKeyPrincipal = new MerchantPrincipal(UUID.randomUUID(), null, true);
        assertThatThrownBy(() -> service.list(apiKeyPrincipal))
                .isInstanceOf(MerchantForbiddenException.class);
    }
}
