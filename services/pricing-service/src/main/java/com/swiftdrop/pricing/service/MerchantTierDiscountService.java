package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.MerchantTier;
import com.swiftdrop.pricing.entity.MerchantTierDiscount;
import com.swiftdrop.pricing.repository.MerchantTierDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Service managing commercial merchant tier volume discount calculations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantTierDiscountService {

    private final MerchantTierDiscountRepository merchantTierDiscountRepository;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * Calculates the contractual merchant tier discount on the zone-adjusted delivery freight.
     * Evaluates active tier configurations. If merchantTier is null or no active discount record exists,
     * returns BigDecimal.ZERO with scale 2.
     *
     * Note: minMonthlyShipments is not evaluated in Step 5 because the service receives only
     * the assigned MerchantTier. Shipment volume tracking and tier qualification reside in merchant-service.
     *
     * @param zoneAdjustedFare the delivery freight after zone multiplier is applied
     * @param merchantTier     the commercial merchant tier (nullable)
     * @return the calculated merchant discount amount (scale 2, RoundingMode.HALF_UP)
     * @throws IllegalArgumentException if zoneAdjustedFare is null or negative
     */
    public BigDecimal calculateDiscount(BigDecimal zoneAdjustedFare, MerchantTier merchantTier) {
        if (zoneAdjustedFare == null) {
            throw new IllegalArgumentException("Zone-adjusted fare cannot be null");
        }
        if (zoneAdjustedFare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Zone-adjusted fare cannot be negative");
        }
        if (merchantTier == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        Optional<MerchantTierDiscount> activeDiscount =
                merchantTierDiscountRepository.findByTierAndIsActiveTrue(merchantTier);

        if (activeDiscount.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal discountPercentage = activeDiscount.get().getDiscountPercentage();
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return zoneAdjustedFare
                .multiply(discountPercentage)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
    }
}
