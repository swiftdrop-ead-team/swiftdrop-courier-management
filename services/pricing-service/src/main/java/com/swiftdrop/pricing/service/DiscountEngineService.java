package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.MerchantTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Service orchestrating commercial merchant volume discounts and promotional coupon discounts,
 * producing the net freight and final delivery fare.
 */
@Service
@Transactional(readOnly = true)
public class DiscountEngineService {

    private final MerchantTierDiscountService merchantTierDiscountService;
    private final PromoCodeService promoCodeService;
    private final Clock clock;

    @Autowired
    public DiscountEngineService(
            MerchantTierDiscountService merchantTierDiscountService,
            PromoCodeService promoCodeService,
            @Autowired(required = false) Clock clock) {
        this.merchantTierDiscountService = merchantTierDiscountService;
        this.promoCodeService = promoCodeService;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public DiscountEngineService(
            MerchantTierDiscountService merchantTierDiscountService,
            PromoCodeService promoCodeService) {
        this(merchantTierDiscountService, promoCodeService, Clock.systemDefaultZone());
    }

    /**
     * Calculates independent merchant and promotional discounts against the zone-adjusted fare,
     * floors net freight at 0.00, and adds the non-discountable fuel surcharge.
     *
     * @param zoneAdjustedFare    the delivery freight after zone multiplier (cannot be null or negative)
     * @param merchantTier        the commercial merchant tier (nullable)
     * @param promoCode           the promotional coupon code (nullable or blank)
     * @param fuelSurchargeAmount the non-discountable fuel surcharge (cannot be null or negative)
     * @param evaluationTime      the explicit evaluation timestamp (cannot be null)
     * @return {@link DiscountEngineResult} with all 7 monetary fields normalized to scale 2
     * @throws IllegalArgumentException if monetary inputs or evaluationTime are invalid
     * @throws IllegalStateException if collaborators return null or negative discount values
     */
    public DiscountEngineResult calculateDiscounts(
            BigDecimal zoneAdjustedFare,
            MerchantTier merchantTier,
            String promoCode,
            BigDecimal fuelSurchargeAmount,
            LocalDateTime evaluationTime) {

        // 1. Validate monetary inputs and evaluation time before calling dependencies
        if (zoneAdjustedFare == null) {
            throw new IllegalArgumentException("Zone-adjusted fare cannot be null");
        }
        if (zoneAdjustedFare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Zone-adjusted fare cannot be negative");
        }
        if (fuelSurchargeAmount == null) {
            throw new IllegalArgumentException("Fuel surcharge amount cannot be null");
        }
        if (fuelSurchargeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fuel surcharge amount cannot be negative");
        }
        if (evaluationTime == null) {
            throw new IllegalArgumentException("Evaluation time cannot be null");
        }

        // 2. Calculate discounts independently against the original zoneAdjustedFare
        BigDecimal merchantDiscount = merchantTierDiscountService.calculateDiscount(zoneAdjustedFare, merchantTier);
        if (merchantDiscount == null || merchantDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Merchant discount returned by service cannot be null or negative");
        }

        BigDecimal promoDiscount = promoCodeService.validateAndCalculateDiscount(promoCode, zoneAdjustedFare, evaluationTime);
        if (promoDiscount == null || promoDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Promo discount returned by service cannot be null or negative");
        }

        // 3. Compute totals and net freight floored at 0.00
        BigDecimal totalDiscountAmount = merchantDiscount.add(promoDiscount);
        BigDecimal netFreight = zoneAdjustedFare.subtract(totalDiscountAmount).max(BigDecimal.ZERO);
        BigDecimal finalFare = netFreight.add(fuelSurchargeAmount);

        return DiscountEngineResult.builder()
                .zoneAdjustedFare(zoneAdjustedFare)
                .merchantDiscountAmount(merchantDiscount)
                .promoDiscountAmount(promoDiscount)
                .totalDiscountAmount(totalDiscountAmount)
                .netFreight(netFreight)
                .fuelSurchargeAmount(fuelSurchargeAmount)
                .finalFare(finalFare)
                .build();
    }

    /**
     * Four-argument overload evaluating against the current clock time.
     *
     * @param zoneAdjustedFare    the delivery freight after zone multiplier
     * @param merchantTier        the commercial merchant tier (nullable)
     * @param promoCode           the promotional coupon code (nullable or blank)
     * @param fuelSurchargeAmount the non-discountable fuel surcharge
     * @return {@link DiscountEngineResult}
     */
    public DiscountEngineResult calculateDiscounts(
            BigDecimal zoneAdjustedFare,
            MerchantTier merchantTier,
            String promoCode,
            BigDecimal fuelSurchargeAmount) {
        return calculateDiscounts(zoneAdjustedFare, merchantTier, promoCode, fuelSurchargeAmount, LocalDateTime.now(clock));
    }
}
