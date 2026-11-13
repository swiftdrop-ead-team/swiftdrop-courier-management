package com.swiftdrop.pricing.strategy;

import com.swiftdrop.pricing.entity.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Value object containing the computed delivery fare breakdown produced by a {@link PricingStrategy}.
 */
@Getter
@ToString
@AllArgsConstructor
@Builder
public class StrategyPricingResult {

    private final ServiceType serviceType;
    private final BigDecimal baseFare;
    private final BigDecimal weightCharge;
    private final BigDecimal distanceCharge;
    private final BigDecimal urgencyMultiplier;
    private final BigDecimal volumetricWeightKg;
    private final BigDecimal billableWeightKg;
    private final BigDecimal extraWeightKg;
    private final BigDecimal subtotalFare;
}
