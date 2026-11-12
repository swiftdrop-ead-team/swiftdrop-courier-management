package com.swiftdrop.merchant.repository;

import com.swiftdrop.merchant.entity.MerchantApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantApiKeyRepository extends JpaRepository<MerchantApiKey, Long> {

    List<MerchantApiKey> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Optional<MerchantApiKey> findByIdAndMerchantId(Long id, UUID merchantId);

    Optional<MerchantApiKey> findByKeyPrefixAndRevokedAtIsNull(String keyPrefix);
}
