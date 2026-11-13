package com.swiftdrop.merchant.repository;

import com.swiftdrop.merchant.entity.WebhookDelivery;
import com.swiftdrop.merchant.entity.WebhookDeliveryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findByWebhookIdOrderByCreatedAtDesc(Long webhookId);

    Optional<WebhookDelivery> findByIdAndWebhookId(Long id, Long webhookId);

    List<WebhookDelivery> findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            WebhookDeliveryStatus status, LocalDateTime cutoff, Pageable pageable);

    @Query("select count(d) from WebhookDelivery d join WebhookSubscription w on w.id = d.webhookId " +
            "where w.merchantId = :merchantId and d.status = :status")
    long countByMerchantIdAndStatus(UUID merchantId, WebhookDeliveryStatus status);
}
