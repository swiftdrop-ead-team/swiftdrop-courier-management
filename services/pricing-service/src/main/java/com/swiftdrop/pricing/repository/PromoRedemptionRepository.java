package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.entity.PromoRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromoRedemptionRepository extends JpaRepository<PromoRedemption, Long> {
    Optional<PromoRedemption> findByOrderReference(String orderReference);
    boolean existsByOrderReference(String orderReference);
    List<PromoRedemption> findByCustomerId(String customerId);
    long countByPromoCodeIdAndCustomerId(Long promoCodeId, String customerId);
}
