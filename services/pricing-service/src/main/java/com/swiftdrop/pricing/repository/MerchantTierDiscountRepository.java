package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.entity.MerchantTier;
import com.swiftdrop.pricing.entity.MerchantTierDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantTierDiscountRepository extends JpaRepository<MerchantTierDiscount, Long> {
    Optional<MerchantTierDiscount> findByTier(MerchantTier tier);
    Optional<MerchantTierDiscount> findByTierAndIsActiveTrue(MerchantTier tier);
    List<MerchantTierDiscount> findByIsActiveTrue();
}
