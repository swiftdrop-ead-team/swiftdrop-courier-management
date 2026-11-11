package com.swiftdrop.pricing.calculator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import java.math.BigDecimal;

/**
 * Simple immutable holder for the intermediate results of the weight calculation.
 */
@Getter
@ToString
@AllArgsConstructor
@Builder
public final class WeightCalculationResult {
    private final BigDecimal volumetricWeightKg;
    private final BigDecimal billableWeightKg;
    private final BigDecimal extraWeightKg;
    private final BigDecimal weightCharge;
}
