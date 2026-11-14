package com.swiftdrop.pricing.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WeightCalculator}. All calculations are performed using {@link BigDecimal}
 * and the rounding rules defined in the pricing design (volumetric weight 4‑decimal, monetary charge 2‑decimal).
 */
class WeightCalculatorTest {

    private static final BigDecimal DIVISOR_5000 = new BigDecimal("5000");
    private static final BigDecimal DIVISOR_4000 = new BigDecimal("4000");
    private static final BigDecimal DIVISOR_2000 = new BigDecimal("2000");
    private static final BigDecimal PER_KG_RATE = new BigDecimal("2.00");
    private static final BigDecimal BASE_WEIGHT = new BigDecimal("2.00");

    /** Helper to assert exact equality with 2‑decimal monetary amounts */
    private static void assertMoneyEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual.setScale(2, RoundingMode.HALF_UP)),
                () -> "Expected monetary " + expected + " but got " + actual);
    }

    @Nested
    @DisplayName("Physical weight greater than volumetric weight")
    class PhysicalGreater {
        @Test
        void calculatesCorrectly() {
            BigDecimal physical = new BigDecimal("10");
            // dimensions: 30x20x10 cm -> volume 6000 / 5000 = 1.2000 kg
            BigDecimal length = new BigDecimal("30");
            BigDecimal width = new BigDecimal("20");
            BigDecimal height = new BigDecimal("10");

            WeightCalculationResult result = WeightCalculator.calculate(
                    physical, length, width, height, DIVISOR_5000, BASE_WEIGHT, PER_KG_RATE);

            assertEquals(new BigDecimal("1.2000"), result.getVolumetricWeightKg());
            assertEquals(physical, result.getBillableWeightKg()); // physical is higher
            assertEquals(new BigDecimal("8.00"), result.getExtraWeightKg()); // 10 - 2
            assertMoneyEquals(new BigDecimal("16.00"), result.getWeightCharge()); // 8 * 2
        }
    }

    @Nested
    @DisplayName("Volumetric weight greater than physical weight")
    class VolumetricGreater {
        @Test
        void calculatesCorrectly() {
            BigDecimal physical = new BigDecimal("3");
            // dimensions: 40x30x25 cm -> volume 30000 / 5000 = 6.0000 kg
            BigDecimal length = new BigDecimal("40");
            BigDecimal width = new BigDecimal("30");
            BigDecimal height = new BigDecimal("25");

            WeightCalculationResult result = WeightCalculator.calculate(
                    physical, length, width, height, DIVISOR_5000, BASE_WEIGHT, PER_KG_RATE);

            assertEquals(new BigDecimal("6.0000"), result.getVolumetricWeightKg());
            assertEquals(new BigDecimal("6.0000"), result.getBillableWeightKg()); // volumetric higher
            assertEquals(new BigDecimal("4.0000"), result.getExtraWeightKg()); // 6 - 2
            assertMoneyEquals(new BigDecimal("8.00"), result.getWeightCharge()); // 4 * 2
        }
    }

    @Nested
    @DisplayName("Physical weight equals volumetric weight")
    class EqualWeights {
        @Test
        void calculatesCorrectly() {
            // dimensions chosen to give 5.0000 kg volumetric weight
            BigDecimal physical = new BigDecimal("5");
            BigDecimal length = new BigDecimal("25"); // 25*20*20 = 10000
            BigDecimal width = new BigDecimal("20");
            BigDecimal height = new BigDecimal("20");
            // divisor 2000 => 10000 / 2000 = 5.0000 kg
            BigDecimal divisor = DIVISOR_2000;

            WeightCalculationResult result = WeightCalculator.calculate(
                    physical, length, width, height, divisor, BASE_WEIGHT, PER_KG_RATE);

            assertEquals(new BigDecimal("5.0000"), result.getVolumetricWeightKg());
            assertEquals(physical, result.getBillableWeightKg());
            assertEquals(new BigDecimal("3.00"), result.getExtraWeightKg()); // 5 - 2
            assertMoneyEquals(new BigDecimal("6.00"), result.getWeightCharge()); // 3 * 2
        }
    }

    @Nested
    @DisplayName("Weight below base weight yields zero extra and charge")
    class BelowBase {
        @Test
        void zeroExtraAndCharge() {
            BigDecimal physical = new BigDecimal("1");
            BigDecimal length = new BigDecimal("10");
            BigDecimal width = new BigDecimal("10");
            BigDecimal height = new BigDecimal("10"); // volumetric = 0.2000 kg

            WeightCalculationResult result = WeightCalculator.calculate(
                    physical, length, width, height, DIVISOR_5000, BASE_WEIGHT, PER_KG_RATE);

            assertEquals(new BigDecimal("0.2000"), result.getVolumetricWeightKg());
            assertEquals(physical, result.getBillableWeightKg()); // physical higher
            assertEquals(BigDecimal.ZERO, result.getExtraWeightKg());
            assertMoneyEquals(BigDecimal.ZERO, result.getWeightCharge());
        }
    }

    @Nested
    @DisplayName("Weight exactly equal to base weight yields zero extra and charge")
    class ExactlyBase {
        @Test
        void zeroExtraAndCharge() {
            BigDecimal physical = new BigDecimal("2");
            // dimensions produce smaller volumetric weight (0.2000 kg)
            BigDecimal length = new BigDecimal("10");
            BigDecimal width = new BigDecimal("10");
            BigDecimal height = new BigDecimal("10");

            WeightCalculationResult result = WeightCalculator.calculate(
                    physical, length, width, height, DIVISOR_5000, BASE_WEIGHT, PER_KG_RATE);

            assertEquals(new BigDecimal("0.2000"), result.getVolumetricWeightKg());
            assertEquals(physical, result.getBillableWeightKg());
            assertEquals(BigDecimal.ZERO, result.getExtraWeightKg());
            assertMoneyEquals(BigDecimal.ZERO, result.getWeightCharge());
        }
    }

    @Nested
    @DisplayName("Volumetric weight rounding edge cases")
    class RoundingEdge {
        @Test
        void roundsHalfUpToFourDecimals() {
            // 33*22*11 = 7986 ; divisor 4537 -> 1.7602 after HALF_UP to 4 decimals
            BigDecimal length = new BigDecimal("33");
            BigDecimal width = new BigDecimal("22");
            BigDecimal height = new BigDecimal("11");
            BigDecimal divisor = new BigDecimal("4537");

            BigDecimal volumetric = WeightCalculator.calculateVolumetricWeight(
                    length, width, height, divisor);
            assertEquals(new BigDecimal("1.7602"), volumetric);
        }
    }

    @Nested
    @DisplayName("Different volumetric divisors produce different results")
    class DifferentDivisors {
        @Test
        void divisorAffectsVolumetricWeight() {
            BigDecimal length = new BigDecimal("20");
            BigDecimal width = new BigDecimal("20");
            BigDecimal height = new BigDecimal("20"); // volume = 8000
            // divisor 4000 -> 2.0000 kg, divisor 5000 -> 1.6000 kg
            BigDecimal vol4000 = WeightCalculator.calculateVolumetricWeight(length, width, height, DIVISOR_4000);
            BigDecimal vol5000 = WeightCalculator.calculateVolumetricWeight(length, width, height, DIVISOR_5000);
            assertEquals(new BigDecimal("2.0000"), vol4000);
            assertEquals(new BigDecimal("1.6000"), vol5000);
        }
    }
    @Nested
    @DisplayName("Invalid divisor handling")
    class InvalidDivisor {
        @Test
        void zeroDivisorThrows() {
            BigDecimal length = BigDecimal.ONE;
            BigDecimal width = BigDecimal.ONE;
            BigDecimal height = BigDecimal.ONE;
            assertThrows(IllegalArgumentException.class, () ->
                WeightCalculator.calculateVolumetricWeight(length, width, height, BigDecimal.ZERO));
        }

        @Test
        void negativeDivisorThrows() {
            BigDecimal length = BigDecimal.ONE;
            BigDecimal width = BigDecimal.ONE;
            BigDecimal height = BigDecimal.ONE;
            assertThrows(IllegalArgumentException.class, () ->
                WeightCalculator.calculateVolumetricWeight(length, width, height, new BigDecimal("-1")));
        }

        @Test
        void nullDivisorThrows() {
            BigDecimal length = BigDecimal.ONE;
            BigDecimal width = BigDecimal.ONE;
            BigDecimal height = BigDecimal.ONE;
            assertThrows(IllegalArgumentException.class, () ->
                WeightCalculator.calculateVolumetricWeight(length, width, height, null));
        }
    }

}
