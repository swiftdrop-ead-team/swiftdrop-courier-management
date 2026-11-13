package com.swiftdrop.merchant.repository;

import com.swiftdrop.merchant.entity.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

    List<WebhookSubscription> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<WebhookSubscription> findByMerchantIdAndActiveTrue(UUID merchantId);

    Optional<WebhookSubscription> findByIdAndMerchantId(Long id, UUID merchantId);
}
