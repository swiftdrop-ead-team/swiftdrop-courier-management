package com.swiftdrop.merchant.repository;

import com.swiftdrop.merchant.entity.MerchantOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantOrderRepository extends JpaRepository<MerchantOrder, Long> {

    Optional<MerchantOrder> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    List<MerchantOrder> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Optional<MerchantOrder> findFirstByParcelIdOrTrackingNumber(String parcelId, String trackingNumber);

    long countByMerchantId(UUID merchantId);

    @Query("select o.deliveryStatus, count(o) from MerchantOrder o where o.merchantId = :merchantId group by o.deliveryStatus")
    List<Object[]> countStatusesByMerchantId(UUID merchantId);
}
