package com.swiftdrop.pricing.strategy;

import com.swiftdrop.pricing.calculator.WeightCalculationResult;
import com.swiftdrop.pricing.calculator.WeightCalculator;
import com.swiftdrop.pricing.entity.PricingRule;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.exception.DeliveryRestrictionException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalTime;

/**
 * Same-Day delivery pricing strategy validating order cutoff times, intra-zone routing,
 * and applying priority / peak surge multipliers without double-multiplication.
 */
@Component
@Getter
public class SameDayDeliveryPricingStrategy implements PricingStrategy {

    private final LocalTime cutoffTime;
    private final BigDecimal defaultSurgeMultiplier;
    private final Clock clock;

    @Autowired
    public SameDayDeliveryPricingStrategy(
            @Value("${pricing.same-day.cutoff-time:14:00}") String cutoffTimeStr,
            @Value("${pricing.same-day.intra-zone-surge-multiplier:1.0000}") BigDecimal defaultSurgeMultiplier,
            @Autowired(required = false) Clock clock) {
        this.cutoffTime = cutoffTimeStr != null ? LocalTime.parse(cutoffTimeStr) : LocalTime.of(14, 0);
        this.defaultSurgeMultiplier = defaultSurgeMultiplier != null ? defaultSurgeMultiplier : BigDecimal.ONE;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public SameDayDeliveryPricingStrategy(LocalTime cutoffTime, BigDecimal defaultSurgeMultiplier, Clock clock) {
        this.cutoffTime = cutoffTime != null ? cutoffTime : LocalTime.of(14, 0);
        this.defaultSurgeMultiplier = defaultSurgeMultiplier != null ? defaultSurgeMultiplier : BigDecimal.ONE;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public SameDayDeliveryPricingStrategy() {
        this(LocalTime.of(14, 0), BigDecimal.ONE, Clock.systemDefaultZone());
    }

    @Override
    public ServiceType getSupportedTier() {
        return ServiceType.SAME_DAY;
    }

    @Override
    public StrategyPricingResult calculate(PricingContext context, PricingRule rule) {
        validateInputs(context, rule);
        validateCutoffTime(context);
        validateIntraZoneEligibility(context);

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

        // Prevent double multiplication: Consolidate rule urgency and surge into a single effective urgency multiplier.
        // Downstream services consume this single consolidated multiplier and do not re-apply urgency scaling.
        BigDecimal surgeFactor = resolveSurgeFactor(context);
        BigDecimal effectiveUrgencyMultiplier = rule.getUrgencyMultiplier()
                .multiply(surgeFactor)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal fareBeforeMultiplier = baseFare
                .add(weightResult.getWeightCharge())
                .add(distanceCharge);

        BigDecimal subtotalFare = fareBeforeMultiplier
                .multiply(effectiveUrgencyMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        return StrategyPricingResult.builder()
                .serviceType(ServiceType.SAME_DAY)
                .baseFare(baseFare)
                .weightCharge(weightResult.getWeightCharge())
                .distanceCharge(distanceCharge)
                .urgencyMultiplier(effectiveUrgencyMultiplier)
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
        if (rule.getServiceType() != ServiceType.SAME_DAY) {
            throw new IllegalArgumentException(String.format(
                    "Mismatched pricing rule: expected %s but got %s",
                    ServiceType.SAME_DAY, rule.getServiceType()
            ));
        }
        if (Boolean.FALSE.equals(rule.getIsActive())) {
            throw new IllegalArgumentException("Pricing rule for SAME_DAY tier is inactive");
        }
    }

    private void validateCutoffTime(PricingContext context) {
        LocalTime evaluationTime = context.getRequestTime() != null
                ? context.getRequestTime().toLocalTime()
                : LocalTime.now(clock);

        if (evaluationTime.isAfter(cutoffTime)) {
            throw new DeliveryRestrictionException(String.format(
                    "Same-day delivery request at %s exceeds daily cutoff time of %s",
                    evaluationTime, cutoffTime
            ));
        }
    }

    private void validateIntraZoneEligibility(PricingContext context) {
        String origin = context.getOriginZone();
        String destination = context.getDestinationZone();

        if (origin == null || destination == null || !origin.trim().equalsIgnoreCase(destination.trim())) {
            throw new DeliveryRestrictionException(String.format(
                    "Same-day delivery is restricted to intra-zone deliveries; origin '%s' and destination '%s' are not in the same zone",
                    origin, destination
            ));
        }
    }

    private BigDecimal resolveSurgeFactor(PricingContext context) {
        if (context.getSurgeMultiplier() != null && context.getSurgeMultiplier().compareTo(BigDecimal.ONE) > 0) {
            return context.getSurgeMultiplier();
        }
        if (defaultSurgeMultiplier != null && defaultSurgeMultiplier.compareTo(BigDecimal.ONE) > 0) {
            return defaultSurgeMultiplier;
        }
        return BigDecimal.ONE;
    }
}
