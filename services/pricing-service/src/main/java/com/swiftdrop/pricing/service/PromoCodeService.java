package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.DiscountType;
import com.swiftdrop.pricing.entity.PromoCode;
import com.swiftdrop.pricing.exception.InvalidPromoCodeException;
import com.swiftdrop.pricing.repository.PromoCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing promotional discount coupon validation and calculation.
 */
@Service
@Transactional(readOnly = true)
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final Clock clock;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Autowired
    public PromoCodeService(
            PromoCodeRepository promoCodeRepository,
            @Autowired(required = false) Clock clock) {
        this.promoCodeRepository = promoCodeRepository;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public PromoCodeService(PromoCodeRepository promoCodeRepository) {
        this(promoCodeRepository, Clock.systemDefaultZone());
    }

    /**
     * Validates an explicitly supplied promo code and calculates the discount amount
     * against the zone-adjusted delivery freight.
     *
     * @param promoCode        the promotional coupon code (nullable or blank for no discount)
     * @param zoneAdjustedFare the delivery freight after zone multiplier is applied
     * @param evaluationTime   the explicit evaluation timestamp (cannot be null)
     * @return the calculated promotional discount amount (scale 2, RoundingMode.HALF_UP)
     * @throws IllegalArgumentException if zoneAdjustedFare is null or negative, or evaluationTime is null
     * @throws InvalidPromoCodeException if promoCode is invalid, ambiguous, inactive, expired, below minOrderAmount, or exceeded usage limit
     * @throws IllegalStateException if the stored promo code configuration is invalid
     */
    public BigDecimal validateAndCalculateDiscount(
            String promoCode,
            BigDecimal zoneAdjustedFare,
            LocalDateTime evaluationTime) {

        if (zoneAdjustedFare == null) {
            throw new IllegalArgumentException("Zone-adjusted fare cannot be null");
        }
        if (zoneAdjustedFare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Zone-adjusted fare cannot be negative");
        }
        if (evaluationTime == null) {
            throw new IllegalArgumentException("Evaluation time cannot be null");
        }
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        String trimmedCode = promoCode.trim();
        List<PromoCode> matches = promoCodeRepository.findByCodeIgnoreCase(trimmedCode);

        if (matches.isEmpty()) {
            throw new InvalidPromoCodeException("Promo code not found: " + promoCode);
        }
        if (matches.size() > 1) {
            throw new InvalidPromoCodeException("Ambiguous promo code: multiple case-variant records found for: " + promoCode);
        }

        PromoCode promo = matches.get(0);
        validatePromoConfiguration(promo);

        if (!Boolean.TRUE.equals(promo.getIsActive())) {
            throw new InvalidPromoCodeException("Promo code is inactive: " + promoCode);
        }

        if (evaluationTime.isBefore(promo.getValidFrom()) || evaluationTime.isAfter(promo.getValidTo())) {
            throw new InvalidPromoCodeException("Promo code is expired or not yet valid: " + promoCode);
        }

        int used = promo.getTimesUsed() != null ? promo.getTimesUsed() : 0;
        if (promo.getUsageLimit() != null && used >= promo.getUsageLimit()) {
            throw new InvalidPromoCodeException("Promo code usage limit has been reached: " + promoCode);
        }

        if (promo.getMinOrderAmount() != null && zoneAdjustedFare.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new InvalidPromoCodeException("Order amount of " + zoneAdjustedFare
                    + " does not meet minimum required amount of " + promo.getMinOrderAmount());
        }

        return calculateDiscountAmount(promo, zoneAdjustedFare);
    }

    /**
     * Overload defaulting evaluation time to current clock time.
     *
     * @param promoCode        the promotional coupon code (nullable or blank for no discount)
     * @param zoneAdjustedFare the delivery freight after zone multiplier is applied
     * @return the calculated promotional discount amount (scale 2, RoundingMode.HALF_UP)
     */
    public BigDecimal validateAndCalculateDiscount(String promoCode, BigDecimal zoneAdjustedFare) {
        return validateAndCalculateDiscount(promoCode, zoneAdjustedFare, LocalDateTime.now(clock));
    }

    private void validatePromoConfiguration(PromoCode promo) {
        if (promo.getDiscountType() == null) {
            throw new IllegalStateException("Invalid promo code configuration: discount type cannot be null for code " + promo.getCode());
        }
        if (promo.getDiscountValue() == null || promo.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invalid promo code configuration: discount value must be strictly positive for code " + promo.getCode());
        }
        if (promo.getDiscountType() == DiscountType.PERCENTAGE && promo.getDiscountValue().compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalStateException("Invalid promo code configuration: percentage discount cannot exceed 100 for code " + promo.getCode());
        }
        if (promo.getMaxDiscountCap() != null && promo.getMaxDiscountCap().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Invalid promo code configuration: max discount cap cannot be negative for code " + promo.getCode());
        }
        if (promo.getValidFrom() == null || promo.getValidTo() == null) {
            throw new IllegalStateException("Invalid promo code configuration: validity dates cannot be null for code " + promo.getCode());
        }
        if (promo.getValidTo().isBefore(promo.getValidFrom())) {
            throw new IllegalStateException("Invalid promo code configuration: validTo cannot be before validFrom for code " + promo.getCode());
        }
    }

    private BigDecimal calculateDiscountAmount(PromoCode promo, BigDecimal zoneAdjustedFare) {
        if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal rawDiscount = zoneAdjustedFare
                    .multiply(promo.getDiscountValue())
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

            if (promo.getMaxDiscountCap() != null && promo.getMaxDiscountCap().compareTo(BigDecimal.ZERO) > 0) {
                rawDiscount = rawDiscount.min(promo.getMaxDiscountCap());
            }
            return rawDiscount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        } else if (promo.getDiscountType() == DiscountType.FLAT_AMOUNT) {
            BigDecimal flatDiscount = promo.getDiscountValue().setScale(2, RoundingMode.HALF_UP);
            return flatDiscount.min(zoneAdjustedFare).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }

        throw new IllegalStateException("Unsupported discount type: " + promo.getDiscountType());
    }
}
