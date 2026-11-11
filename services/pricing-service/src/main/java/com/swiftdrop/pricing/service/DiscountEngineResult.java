package com.swiftdrop.pricing.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable value object containing the discount calculation and fare breakdown.
 * All seven monetary fields are strictly normalized to scale 2 using {@link RoundingMode#HALF_UP}.
 */
@Getter
@ToString
@EqualsAndHashCode
public class DiscountEngineResult {

    private final BigDecimal zoneAdjustedFare;
    private final BigDecimal merchantDiscountAmount;
    private final BigDecimal promoDiscountAmount;
    private final BigDecimal totalDiscountAmount;
    private final BigDecimal netFreight;
    private final BigDecimal fuelSurchargeAmount;
    private final BigDecimal finalFare;

    public DiscountEngineResult(
            BigDecimal zoneAdjustedFare,
            BigDecimal merchantDiscountAmount,
            BigDecimal promoDiscountAmount,
            BigDecimal totalDiscountAmount,
            BigDecimal netFreight,
            BigDecimal fuelSurchargeAmount,
            BigDecimal finalFare) {
        this.zoneAdjustedFare = normalize(zoneAdjustedFare, "zoneAdjustedFare");
        this.merchantDiscountAmount = normalize(merchantDiscountAmount, "merchantDiscountAmount");
        this.promoDiscountAmount = normalize(promoDiscountAmount, "promoDiscountAmount");
        this.totalDiscountAmount = normalize(totalDiscountAmount, "totalDiscountAmount");
        this.netFreight = normalize(netFreight, "netFreight");
        this.fuelSurchargeAmount = normalize(fuelSurchargeAmount, "fuelSurchargeAmount");
        this.finalFare = normalize(finalFare, "finalFare");
    }

    private static BigDecimal normalize(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal zoneAdjustedFare;
        private BigDecimal merchantDiscountAmount;
        private BigDecimal promoDiscountAmount;
        private BigDecimal totalDiscountAmount;
        private BigDecimal netFreight;
        private BigDecimal fuelSurchargeAmount;
        private BigDecimal finalFare;

        public Builder zoneAdjustedFare(BigDecimal zoneAdjustedFare) {
            this.zoneAdjustedFare = zoneAdjustedFare;
            return this;
        }

        public Builder merchantDiscountAmount(BigDecimal merchantDiscountAmount) {
            this.merchantDiscountAmount = merchantDiscountAmount;
            return this;
        }

        public Builder promoDiscountAmount(BigDecimal promoDiscountAmount) {
            this.promoDiscountAmount = promoDiscountAmount;
            return this;
        }

        public Builder totalDiscountAmount(BigDecimal totalDiscountAmount) {
            this.totalDiscountAmount = totalDiscountAmount;
            return this;
        }

        public Builder netFreight(BigDecimal netFreight) {
            this.netFreight = netFreight;
            return this;
        }

        public Builder fuelSurchargeAmount(BigDecimal fuelSurchargeAmount) {
            this.fuelSurchargeAmount = fuelSurchargeAmount;
            return this;
        }

        public Builder finalFare(BigDecimal finalFare) {
            this.finalFare = finalFare;
            return this;
        }

        public DiscountEngineResult build() {
            return new DiscountEngineResult(
                    zoneAdjustedFare,
                    merchantDiscountAmount,
                    promoDiscountAmount,
                    totalDiscountAmount,
                    netFreight,
                    fuelSurchargeAmount,
                    finalFare
            );
        }
    }
}
