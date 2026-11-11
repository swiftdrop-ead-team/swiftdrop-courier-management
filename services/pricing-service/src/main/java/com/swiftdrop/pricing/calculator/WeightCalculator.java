package com.swiftdrop.pricing.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure calculation component for weight‑related pricing figures.
 *
 * <p>All calculations use {@link BigDecimal} to avoid floating‑point errors and
 * apply {@link RoundingMode#HALF_UP} as defined in the pricing design.</p>
 */
public final class WeightCalculator {

    private WeightCalculator() {
        // Utility class – prevent instantiation.
    }

    /**
     * Calculates the volumetric weight.
     *
     * @param lengthCm            length in centimetres (must be &gt; 0)
     * @param widthCm             width in centimetres (must be &gt; 0)
     * @param heightCm            height in centimetres (must be &gt; 0)
     * @param volumetricDivisor   divisor specified by the {@code PricingRule}
     * @return volumetric weight in kilograms, rounded to 2 decimal places
     */
    public static BigDecimal calculateVolumetricWeight(
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            BigDecimal volumetricDivisor) {
        if (volumetricDivisor == null || volumetricDivisor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Volumetric divisor must be a positive number");
        }
        return lengthCm.multiply(widthCm)
                .multiply(heightCm)
                .divide(volumetricDivisor, 4, RoundingMode.HALF_UP);
    }

    /**
     * Determines the billable weight – the greater of physical and volumetric weight.
     */
    public static BigDecimal calculateBillableWeight(
            BigDecimal physicalWeightKg,
            BigDecimal volumetricWeightKg) {
        return physicalWeightKg.max(volumetricWeightKg);
    }

    /**
     * Calculates the extra weight that exceeds the base weight.
     *
     * @return {@code 0} when the billable weight is less than or equal to the base weight.
     */
    public static BigDecimal calculateExtraWeight(
            BigDecimal billableWeightKg,
            BigDecimal baseWeightKg) {
        BigDecimal diff = billableWeightKg.subtract(baseWeightKg);
        if (diff.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return diff;
    }

    /**
     * Computes the weight‑based charge.
     *
     * @param extraWeightKg weight above the base weight
     * @param perKgRate    monetary rate per kilogram
     * @return charge rounded to 2 decimal places (currency scale)
     */
    public static BigDecimal calculateWeightCharge(
            BigDecimal extraWeightKg,
            BigDecimal perKgRate) {
        return extraWeightKg.multiply(perKgRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convenience method that performs the whole weight calculation pipeline.
     *
     * @return a {@link WeightCalculationResult} holding every intermediate value.
     */
    public static WeightCalculationResult calculate(
            BigDecimal physicalWeightKg,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            BigDecimal volumetricDivisor,
            BigDecimal baseWeightKg,
            BigDecimal perKgRate) {
        BigDecimal volumetricWeightKg = calculateVolumetricWeight(lengthCm, widthCm, heightCm, volumetricDivisor);
        BigDecimal billableWeightKg = calculateBillableWeight(physicalWeightKg, volumetricWeightKg);
        BigDecimal extraWeightKg = calculateExtraWeight(billableWeightKg, baseWeightKg);
        BigDecimal weightCharge = calculateWeightCharge(extraWeightKg, perKgRate);
        return new WeightCalculationResult(volumetricWeightKg, billableWeightKg, extraWeightKg, weightCharge);
    }
}
