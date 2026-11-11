package com.swiftdrop.pricing.strategy;

import com.swiftdrop.pricing.entity.PricingRule;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.exception.DeliveryRestrictionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ExpressDeliveryPricingStrategyTest {

    private ExpressDeliveryPricingStrategy strategy;
    private PricingRule expressRule;

    @BeforeEach
    void setUp() {
        strategy = new ExpressDeliveryPricingStrategy();
        expressRule = PricingRule.builder()
                .serviceType(ServiceType.EXPRESS)
                .baseFare(new BigDecimal("500.00"))
                .baseWeightKg(new BigDecimal("1.00"))
                .perKgRate(new BigDecimal("100.00"))
                .perKmRate(new BigDecimal("35.00"))
                .volumetricDivisor(5000)
                .urgencyMultiplier(new BigDecimal("1.3500"))
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Strategy returns ServiceType.EXPRESS as supported tier")
    void testGetSupportedTier() {
        assertEquals(ServiceType.EXPRESS, strategy.getSupportedTier());
    }

    @Test
    @DisplayName("Express pricing applies urgency multiplier to calculate subtotal")
    void testNormalExpressCalculation() {
        // Physical: 2.00 kg, Dimensions: 30x20x10 cm (volumetric: 1.2000 kg), Distance: 10 km
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("2.00"))
                .lengthCm(new BigDecimal("30"))
                .widthCm(new BigDecimal("20"))
                .heightCm(new BigDecimal("10"))
                .distanceKm(new BigDecimal("10.00"))
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("GREATER_COLOMBO")
                .build();

        StrategyPricingResult result = strategy.calculate(context, expressRule);

        assertNotNull(result);
        assertEquals(ServiceType.EXPRESS, result.getServiceType());
        assertEquals(0, new BigDecimal("500.00").compareTo(result.getBaseFare()));

        // Extra weight = 2.00 - 1.00 = 1.00 kg * 100.00 = 100.00
        assertEquals(0, new BigDecimal("100.00").compareTo(result.getWeightCharge()));

        // Distance charge = 10.00 km * 35.00 = 350.00
        assertEquals(0, new BigDecimal("350.00").compareTo(result.getDistanceCharge()));

        assertEquals(0, new BigDecimal("1.3500").compareTo(result.getUrgencyMultiplier()));

        // Subtotal = (500 + 100 + 350) * 1.3500 = 950 * 1.3500 = 1282.50
        assertEquals(0, new BigDecimal("1282.50").compareTo(result.getSubtotalFare()));
    }

    @Test
    @DisplayName("Physical weight exactly at boundary (30.00 kg) is accepted")
    void testWeightAtExactBoundaryPasses() {
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("30.00"))
                .lengthCm(new BigDecimal("20"))
                .widthCm(new BigDecimal("20"))
                .heightCm(new BigDecimal("10"))
                .distanceKm(new BigDecimal("5.00"))
                .build();

        assertDoesNotThrow(() -> strategy.calculate(context, expressRule));
    }

    @Test
    @DisplayName("Physical weight exceeding 30.00 kg throws DeliveryRestrictionException")
    void testPhysicalWeightExceedingLimitThrows() {
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("30.01"))
                .lengthCm(new BigDecimal("20"))
                .widthCm(new BigDecimal("20"))
                .heightCm(new BigDecimal("10"))
                .distanceKm(new BigDecimal("5.00"))
                .build();

        DeliveryRestrictionException ex = assertThrows(DeliveryRestrictionException.class, () ->
                strategy.calculate(context, expressRule));
        assertTrue(ex.getMessage().contains("exceeds express maximum weight limit"));
    }

    @Test
    @DisplayName("Volumetric billable weight exceeding 30.00 kg throws DeliveryRestrictionException")
    void testVolumetricWeightExceedingLimitThrows() {
        // Physical: 5.00 kg, Dimensions: 100x80x25 cm -> volume: 200,000 / 5000 = 40.0000 kg (> 30 kg)
        // Note: combined girth = 100 + 2*(80+25) = 310 > 200, so test with 80x50x40 = 160,000/5000 = 32 kg
        // girth: 80 + 2*(25+20) = 170 <= 200 cm, volume: 80x50x50 = 200,000 / 5000 = 40 kg.
        // Let's use: length=60, width=50, height=60 -> 180,000 / 5000 = 36.00 kg (> 30 kg limit)
        // girth: 60 + 2*(30+35) = 190 cm (< 200 cm girth limit)
        // volume: 60 x 60 x 50 = 180,000 / 5000 = 36.00 kg
        // girth = 60 + 2*(30+35) = 190 cm
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("5.00"))
                .lengthCm(new BigDecimal("60"))
                .widthCm(new BigDecimal("35"))
                .heightCm(new BigDecimal("30")) // girth = 60 + 2*(35+30) = 190 cm <= 200 cm
                .distanceKm(new BigDecimal("5.00"))
                .build();
        // Here volumetric weight = 60*35*30 / 5000 = 12.6 kg. Let's make volumetric > 30 kg within 200 cm girth:
        // length = 100, width = 25, height = 24 -> girth = 100 + 2*(49) = 198 cm <= 200 cm
        // volume = 100 * 25 * 24 = 60,000 / 5000 = 12 kg.
        // Wait, max weight 30 kg with custom strategy:
        ExpressDeliveryPricingStrategy customStrategy = new ExpressDeliveryPricingStrategy(
                new BigDecimal("10.00"), new BigDecimal("120.00"), new BigDecimal("300.00")
        );
        PricingContext context2 = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("2.00"))
                .lengthCm(new BigDecimal("50"))
                .widthCm(new BigDecimal("40"))
                .heightCm(new BigDecimal("30")) // volumetric: 60,000 / 5000 = 12.00 kg (> 10.00 kg)
                .distanceKm(new BigDecimal("5.00"))
                .build();

        DeliveryRestrictionException ex = assertThrows(DeliveryRestrictionException.class, () ->
                customStrategy.calculate(context2, expressRule));
        assertTrue(ex.getMessage().contains("Billable weight"));
    }

    @Test
    @DisplayName("Dimension exactly at length boundary (120.00 cm) is accepted")
    void testLengthAtExactBoundaryPasses() {
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("5.00"))
                .lengthCm(new BigDecimal("120.00"))
                .widthCm(new BigDecimal("15.00"))
                .heightCm(new BigDecimal("10.00")) // combined girth = 120 + 2*(15+10) = 170.00 <= 200
                .distanceKm(new BigDecimal("5.00"))
                .build();

        assertDoesNotThrow(() -> strategy.calculate(context, expressRule));
    }

    @Test
    @DisplayName("Dimension exceeding 120.00 cm length limit throws DeliveryRestrictionException")
    void testLengthExceedingLimitThrows() {
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("5.00"))
                .lengthCm(new BigDecimal("120.01"))
                .widthCm(new BigDecimal("10.00"))
                .heightCm(new BigDecimal("10.00"))
                .distanceKm(new BigDecimal("5.00"))
                .build();

        DeliveryRestrictionException ex = assertThrows(DeliveryRestrictionException.class, () ->
                strategy.calculate(context, expressRule));
        assertTrue(ex.getMessage().contains("exceeds express maximum length limit"));
    }

    @Test
    @DisplayName("Width exceeding 120.00 cm length limit throws DeliveryRestrictionException")
    void testWidthExceedingMaxDimensionThrows() {
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("5.00"))
                .lengthCm(new BigDecimal("50.00"))
                .widthCm(new BigDecimal("125.00"))
                .heightCm(new BigDecimal("10.00"))
                .distanceKm(new BigDecimal("5.00"))
                .build();

        DeliveryRestrictionException ex = assertThrows(DeliveryRestrictionException.class, () ->
                strategy.calculate(context, expressRule));
        assertTrue(ex.getMessage().contains("exceeds express maximum length limit"));
    }

    @Test
    @DisplayName("Combined girth exactly at boundary (200.00 cm) is accepted")
    void testCombinedGirthAtExactBoundaryPasses() {
        // Girth = length + 2*(width + height) = 100 + 2*(30 + 20) = 200.00 cm
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("5.00"))
                .lengthCm(new BigDecimal("100.00"))
                .widthCm(new BigDecimal("30.00"))
                .heightCm(new BigDecimal("20.00"))
                .distanceKm(new BigDecimal("5.00"))
                .build();

        assertDoesNotThrow(() -> strategy.calculate(context, expressRule));
    }

    @Test
    @DisplayName("Combined girth exceeding 200.00 cm throws DeliveryRestrictionException")
    void testCombinedGirthExceedingLimitThrows() {
        // Girth = 100 + 2*(30 + 20.01) = 200.02 cm
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("5.00"))
                .lengthCm(new BigDecimal("100.00"))
                .widthCm(new BigDecimal("30.00"))
                .heightCm(new BigDecimal("20.01"))
                .distanceKm(new BigDecimal("5.00"))
                .build();

        DeliveryRestrictionException ex = assertThrows(DeliveryRestrictionException.class, () ->
                strategy.calculate(context, expressRule));
        assertTrue(ex.getMessage().contains("exceeds express maximum limit"));
    }

    @Test
    @DisplayName("Custom configurable limits take effect when injected via constructor")
    void testCustomConfigurableLimits() {
        ExpressDeliveryPricingStrategy customStrategy = new ExpressDeliveryPricingStrategy(
                new BigDecimal("15.00"), new BigDecimal("80.00"), new BigDecimal("150.00")
        );

        assertEquals(new BigDecimal("15.00"), customStrategy.getMaxWeightKg());
        assertEquals(new BigDecimal("80.00"), customStrategy.getMaxLengthCm());
        assertEquals(new BigDecimal("150.00"), customStrategy.getMaxCombinedGirthCm());

        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("16.00"))
                .lengthCm(new BigDecimal("50.00"))
                .widthCm(new BigDecimal("20.00"))
                .heightCm(new BigDecimal("10.00"))
                .distanceKm(new BigDecimal("5.00"))
                .build();

        assertThrows(DeliveryRestrictionException.class, () ->
                customStrategy.calculate(context, expressRule));
    }

    @Test
    @DisplayName("Mismatched PricingRule throws IllegalArgumentException")
    void testMismatchedServiceTypeThrows() {
        PricingRule standardRule = PricingRule.builder()
                .serviceType(ServiceType.STANDARD)
                .build();

        PricingContext context = PricingContext.builder()
                .physicalWeightKg(BigDecimal.ONE)
                .lengthCm(BigDecimal.TEN)
                .widthCm(BigDecimal.TEN)
                .heightCm(BigDecimal.TEN)
                .distanceKm(BigDecimal.TEN)
                .build();

        assertThrows(IllegalArgumentException.class, () -> strategy.calculate(context, standardRule));
    }
}
