package com.swiftdrop.pricing.strategy;

import com.swiftdrop.pricing.calculator.WeightCalculator;
import com.swiftdrop.pricing.entity.PricingRule;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.exception.DeliveryRestrictionException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Express delivery pricing strategy enforcing express rate metrics and package weight/dimension limits.
 */
@Component
@Getter
public class ExpressDeliveryPricingStrategy implements PricingStrategy {

    private final BigDecimal maxWeightKg;
    private final BigDecimal maxLengthCm;
    private final BigDecimal maxCombinedGirthCm;

    public ExpressDeliveryPricingStrategy(
            @Value("${pricing.express.max-weight-kg:30.00}") BigDecimal maxWeightKg,
            @Value("${pricing.express.max-length-cm:120.00}") BigDecimal maxLengthCm,
            @Value("${pricing.express.max-combined-girth-cm:200.00}") BigDecimal maxCombinedGirthCm) {
        this.maxWeightKg = maxWeightKg != null ? maxWeightKg : new BigDecimal("30.00");
        this.maxLengthCm = maxLengthCm != null ? maxLengthCm : new BigDecimal("120.00");
        this.maxCombinedGirthCm = maxCombinedGirthCm != null ? maxCombinedGirthCm : new BigDecimal("200.00");
    }

    public ExpressDeliveryPricingStrategy() {
        this(new BigDecimal("30.00"), new BigDecimal("120.00"), new BigDecimal("200.00"));
    }

    @Override
    public ServiceType getSupportedTier() {
        return ServiceType.EXPRESS;
    }

    @Override
    public StrategyPricingResult calculate(PricingContext context, PricingRule rule) {
        validateInputs(context, rule);
        validatePackageDimensions(context);

        BigDecimal volumetricDivisor = BigDecimal.valueOf(rule.getVolumetricDivisor());
        BigDecimal volumetricWeight = WeightCalculator.calculateVolumetricWeight(
                context.getLengthCm(),
                context.getWidthCm(),
                context.getHeightCm(),
                volumetricDivisor
        );
        BigDecimal billableWeight = WeightCalculator.calculateBillableWeight(
                context.getPhysicalWeightKg(),
                volumetricWeight
        );

        validatePackageWeight(context.getPhysicalWeightKg(), billableWeight);

        BigDecimal extraWeight = WeightCalculator.calculateExtraWeight(billableWeight, rule.getBaseWeightKg());
        BigDecimal weightCharge = WeightCalculator.calculateWeightCharge(extraWeight, rule.getPerKgRate());

        BigDecimal distanceCharge = context.getDistanceKm()
                .multiply(rule.getPerKmRate())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal baseFare = rule.getBaseFare();
        BigDecimal urgencyMultiplier = rule.getUrgencyMultiplier();

        BigDecimal fareBeforeMultiplier = baseFare
                .add(weightCharge)
                .add(distanceCharge);

        BigDecimal subtotalFare = fareBeforeMultiplier
                .multiply(urgencyMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        return StrategyPricingResult.builder()
                .serviceType(ServiceType.EXPRESS)
                .baseFare(baseFare)
                .weightCharge(weightCharge)
                .distanceCharge(distanceCharge)
                .urgencyMultiplier(urgencyMultiplier)
                .volumetricWeightKg(volumetricWeight)
                .billableWeightKg(billableWeight)
                .extraWeightKg(extraWeight)
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
        if (rule.getServiceType() != ServiceType.EXPRESS) {
            throw new IllegalArgumentException(String.format(
                    "Mismatched pricing rule: expected %s but got %s",
                    ServiceType.EXPRESS, rule.getServiceType()
            ));
        }
        if (Boolean.FALSE.equals(rule.getIsActive())) {
            throw new IllegalArgumentException("Pricing rule for EXPRESS tier is inactive");
        }
    }

    private void validatePackageDimensions(PricingContext context) {
        BigDecimal length = context.getLengthCm();
        BigDecimal width = context.getWidthCm();
        BigDecimal height = context.getHeightCm();

        BigDecimal maxDimension = length.max(width).max(height);
        if (maxDimension.compareTo(maxLengthCm) > 0) {
            throw new DeliveryRestrictionException(String.format(
                    "Package dimension %.2f cm exceeds express maximum length limit of %.2f cm",
                    maxDimension, maxLengthCm
            ));
        }

        // Combined girth: length + 2 * (width + height)
        BigDecimal combinedGirth = length.add(
                width.add(height).multiply(BigDecimal.valueOf(2))
        );
        if (combinedGirth.compareTo(maxCombinedGirthCm) > 0) {
            throw new DeliveryRestrictionException(String.format(
                    "Package combined girth %.2f cm exceeds express maximum limit of %.2f cm",
                    combinedGirth, maxCombinedGirthCm
            ));
        }
    }

    private void validatePackageWeight(BigDecimal physicalWeight, BigDecimal billableWeight) {
        if (physicalWeight.compareTo(maxWeightKg) > 0) {
            throw new DeliveryRestrictionException(String.format(
                    "Physical weight %.2f kg exceeds express maximum weight limit of %.2f kg",
                    physicalWeight, maxWeightKg
            ));
        }
        if (billableWeight.compareTo(maxWeightKg) > 0) {
            throw new DeliveryRestrictionException(String.format(
                    "Billable weight %.2f kg exceeds express maximum weight limit of %.2f kg",
                    billableWeight, maxWeightKg
            ));
        }
    }
}
