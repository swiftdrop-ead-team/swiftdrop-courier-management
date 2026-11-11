package com.swiftdrop.pricing.strategy;

import com.swiftdrop.pricing.entity.PricingRule;
import com.swiftdrop.pricing.entity.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class StandardDeliveryPricingStrategyTest {

    private StandardDeliveryPricingStrategy strategy;
    private PricingRule standardRule;

    @BeforeEach
    void setUp() {
        strategy = new StandardDeliveryPricingStrategy();
        standardRule = PricingRule.builder()
                .serviceType(ServiceType.STANDARD)
                .baseFare(new BigDecimal("350.00"))
                .baseWeightKg(new BigDecimal("1.00"))
                .perKgRate(new BigDecimal("75.00"))
                .perKmRate(new BigDecimal("25.00"))
                .volumetricDivisor(5000)
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Strategy returns ServiceType.STANDARD as supported tier")
    void testGetSupportedTier() {
        assertEquals(ServiceType.STANDARD, strategy.getSupportedTier());
    }

    @Test
    @DisplayName("Standard pricing calculates base fare, extra weight charge, and distance charge correctly")
    void testNormalCalculationWithExtraWeight() {
        // Physical: 3.50 kg, Dimensions: 20x20x10 cm (volumetric: 0.8000 kg), Distance: 10 km
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("3.50"))
                .lengthCm(new BigDecimal("20"))
                .widthCm(new BigDecimal("20"))
                .heightCm(new BigDecimal("10"))
                .distanceKm(new BigDecimal("10.00"))
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("COLOMBO_CENTRAL")
                .build();

        StrategyPricingResult result = strategy.calculate(context, standardRule);

        assertNotNull(result);
        assertEquals(ServiceType.STANDARD, result.getServiceType());
        assertEquals(0, new BigDecimal("350.00").compareTo(result.getBaseFare()));

        // Billable weight = 3.50 kg, Base = 1.00 kg -> Extra = 2.50 kg * 75.00 = 187.50
        assertEquals(0, new BigDecimal("187.50").compareTo(result.getWeightCharge()));

        // Distance charge = 10.00 km * 25.00 = 250.00
        assertEquals(0, new BigDecimal("250.00").compareTo(result.getDistanceCharge()));

        assertEquals(0, new BigDecimal("1.0000").compareTo(result.getUrgencyMultiplier()));

        // Subtotal = (350 + 187.50 + 250) * 1.0 = 787.50
        assertEquals(0, new BigDecimal("787.50").compareTo(result.getSubtotalFare()));
    }

    @Test
    @DisplayName("Package within base weight produces zero weight charge")
    void testCalculationWithinBaseWeight() {
        // Physical: 0.50 kg, Dimensions: 10x10x10 cm (volumetric: 0.2000 kg), Distance: 5 km
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("0.50"))
                .lengthCm(new BigDecimal("10"))
                .widthCm(new BigDecimal("10"))
                .heightCm(new BigDecimal("10"))
                .distanceKm(new BigDecimal("5.00"))
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("GREATER_COLOMBO")
                .build();

        StrategyPricingResult result = strategy.calculate(context, standardRule);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getExtraWeightKg()));
        assertEquals(0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).compareTo(result.getWeightCharge()));

        // Subtotal = (350 + 0 + 125.00) * 1.0 = 475.00
        assertEquals(0, new BigDecimal("475.00").compareTo(result.getSubtotalFare()));
    }

    @Test
    @DisplayName("Volumetric weight exceeding physical weight is used as billable weight")
    void testVolumetricWeightDominates() {
        // Physical: 1.00 kg, Dimensions: 50x40x30 cm -> volume: 60,000 / 5000 = 12.0000 kg
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("1.00"))
                .lengthCm(new BigDecimal("50"))
                .widthCm(new BigDecimal("40"))
                .heightCm(new BigDecimal("30"))
                .distanceKm(new BigDecimal("12.00"))
                .originZone("ZONE_A")
                .destinationZone("ZONE_B")
                .build();

        StrategyPricingResult result = strategy.calculate(context, standardRule);

        assertEquals(0, new BigDecimal("12.0000").compareTo(result.getVolumetricWeightKg()));
        assertEquals(0, new BigDecimal("12.0000").compareTo(result.getBillableWeightKg()));
        // Extra = 12.00 - 1.00 = 11.00 kg * 75.00 = 825.00
        assertEquals(0, new BigDecimal("825.00").compareTo(result.getWeightCharge()));
    }

    @Test
    @DisplayName("Mismatched PricingRule service type throws IllegalArgumentException")
    void testMismatchedServiceTypeThrows() {
        PricingRule expressRule = PricingRule.builder()
                .serviceType(ServiceType.EXPRESS)
                .baseFare(new BigDecimal("500.00"))
                .build();

        PricingContext context = PricingContext.builder()
                .physicalWeightKg(BigDecimal.ONE)
                .lengthCm(BigDecimal.TEN)
                .widthCm(BigDecimal.TEN)
                .heightCm(BigDecimal.TEN)
                .distanceKm(BigDecimal.TEN)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                strategy.calculate(context, expressRule));
        assertTrue(ex.getMessage().contains("Mismatched pricing rule"));
    }

    @Test
    @DisplayName("Inactive PricingRule throws IllegalArgumentException")
    void testInactiveRuleThrows() {
        standardRule.setIsActive(false);

        PricingContext context = PricingContext.builder()
                .physicalWeightKg(BigDecimal.ONE)
                .lengthCm(BigDecimal.TEN)
                .widthCm(BigDecimal.TEN)
                .heightCm(BigDecimal.TEN)
                .distanceKm(BigDecimal.TEN)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                strategy.calculate(context, standardRule));
        assertTrue(ex.getMessage().contains("inactive"));
    }

    @Test
    @DisplayName("Null context or rule throws IllegalArgumentException")
    void testNullInputsThrow() {
        assertThrows(IllegalArgumentException.class, () -> strategy.calculate(null, standardRule));

        PricingContext context = PricingContext.builder()
                .physicalWeightKg(BigDecimal.ONE)
                .lengthCm(BigDecimal.TEN)
                .widthCm(BigDecimal.TEN)
                .heightCm(BigDecimal.TEN)
                .distanceKm(BigDecimal.TEN)
                .build();
        assertThrows(IllegalArgumentException.class, () -> strategy.calculate(context, null));
    }
}
