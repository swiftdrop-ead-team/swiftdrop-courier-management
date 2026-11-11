package com.swiftdrop.pricing.strategy;

import com.swiftdrop.pricing.calculator.WeightCalculationResult;
import com.swiftdrop.pricing.calculator.WeightCalculator;
import com.swiftdrop.pricing.entity.PricingRule;
import com.swiftdrop.pricing.entity.ServiceType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Standard delivery pricing strategy implementing baseline pricing rules.
 */
@Component
public class StandardDeliveryPricingStrategy implements PricingStrategy {

    @Override
    public ServiceType getSupportedTier() {
        return ServiceType.STANDARD;
    }

    @Override
    public StrategyPricingResult calculate(PricingContext context, PricingRule rule) {
        validateInputs(context, rule);

        WeightCalculationResult weightResult = WeightCalculator.calculate(
                context.getPhysicalWeightKg(),
                context.getLengthCm(),
                context.getWidthCm(),
                context.getHeightCm(),
                BigDecimal.valueOf(rule.getVolumetricDivisor()),
                rule.getBaseWeightKg(),
                rule.getPerKgRate()
        );

        BigDecimal distanceCharge = context.getDistanceKm()
                .multiply(rule.getPerKmRate())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal baseFare = rule.getBaseFare();
        BigDecimal urgencyMultiplier = rule.getUrgencyMultiplier();

        BigDecimal fareBeforeMultiplier = baseFare
                .add(weightResult.getWeightCharge())
                .add(distanceCharge);

        BigDecimal subtotalFare = fareBeforeMultiplier
                .multiply(urgencyMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        return StrategyPricingResult.builder()
                .serviceType(ServiceType.STANDARD)
                .baseFare(baseFare)
                .weightCharge(weightResult.getWeightCharge())
                .distanceCharge(distanceCharge)
                .urgencyMultiplier(urgencyMultiplier)
                .volumetricWeightKg(weightResult.getVolumetricWeightKg())
                .billableWeightKg(weightResult.getBillableWeightKg())
                .extraWeightKg(weightResult.getExtraWeightKg())
                .subtotalFare(subtotalFare)
                .build();
    }

    private void validateInputs(PricingContext context, PricingRule rule) {
        if (context == null) {
            throw new IllegalArgumentException("Pricing context cannot be null");
        }
        if (rule == null) {
            throw new IllegalArgumentException("Pricing rule cannot be null");
        }
        if (rule.getServiceType() != ServiceType.STANDARD) {
            throw new IllegalArgumentException(String.format(
                    "Mismatched pricing rule: expected %s but got %s",
                    ServiceType.STANDARD, rule.getServiceType()
            ));
        }
        if (Boolean.FALSE.equals(rule.getIsActive())) {
            throw new IllegalArgumentException("Pricing rule for STANDARD tier is inactive");
        }
    }
}
