package com.swiftdrop.pricing.strategy;

import com.swiftdrop.pricing.entity.PricingRule;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.exception.DeliveryRestrictionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class SameDayDeliveryPricingStrategyTest {

    private SameDayDeliveryPricingStrategy strategy;
    private PricingRule sameDayRule;
    private final ZoneId zoneId = ZoneId.of("Asia/Colombo");

    @BeforeEach
    void setUp() {
        strategy = new SameDayDeliveryPricingStrategy();
        sameDayRule = PricingRule.builder()
                .serviceType(ServiceType.SAME_DAY)
                .baseFare(new BigDecimal("700.00"))
                .baseWeightKg(new BigDecimal("1.00"))
                .perKgRate(new BigDecimal("150.00"))
                .perKmRate(new BigDecimal("50.00"))
                .volumetricDivisor(5000)
                .urgencyMultiplier(new BigDecimal("1.5000"))
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Strategy returns ServiceType.SAME_DAY as supported tier")
    void testGetSupportedTier() {
        assertEquals(ServiceType.SAME_DAY, strategy.getSupportedTier());
    }

    @Test
    @DisplayName("Normal same-day calculation applies urgency multiplier without surge")
    void testNormalSameDayCalculation() {
        LocalDateTime requestTime = LocalDateTime.of(2026, 11, 10, 10, 30, 0);
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("2.00"))
                .lengthCm(new BigDecimal("20"))
                .widthCm(new BigDecimal("20"))
                .heightCm(new BigDecimal("10"))
                .distanceKm(new BigDecimal("8.00"))
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("COLOMBO_CENTRAL")
                .requestTime(requestTime)
                .build();

        StrategyPricingResult result = strategy.calculate(context, sameDayRule);

        assertNotNull(result);
        assertEquals(ServiceType.SAME_DAY, result.getServiceType());
        assertEquals(0, new BigDecimal("700.00").compareTo(result.getBaseFare()));

        // Extra weight = 2.00 - 1.00 = 1.00 kg * 150.00 = 150.00
        assertEquals(0, new BigDecimal("150.00").compareTo(result.getWeightCharge()));

        // Distance charge = 8.00 * 50.00 = 400.00
        assertEquals(0, new BigDecimal("400.00").compareTo(result.getDistanceCharge()));

        assertEquals(0, new BigDecimal("1.5000").compareTo(result.getUrgencyMultiplier()));

        // Subtotal = (700 + 150 + 400) * 1.5000 = 1250 * 1.5000 = 1875.00
        assertEquals(0, new BigDecimal("1875.00").compareTo(result.getSubtotalFare()));
    }

    @Test
    @DisplayName("Request time before cutoff (13:59:59) is accepted")
    void testRequestBeforeCutoffPasses() {
        LocalDateTime beforeCutoff = LocalDateTime.of(2026, 11, 10, 13, 59, 59);
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(BigDecimal.ONE)
                .lengthCm(BigDecimal.TEN)
                .widthCm(BigDecimal.TEN)
                .heightCm(BigDecimal.TEN)
                .distanceKm(new BigDecimal("5.00"))
                .originZone("ZONE_1")
                .destinationZone("ZONE_1")
                .requestTime(beforeCutoff)
                .build();

        assertDoesNotThrow(() -> strategy.calculate(context, sameDayRule));
    }

    @Test
    @DisplayName("Request time exactly at cutoff (14:00:00) is accepted")
    void testRequestAtExactCutoffPasses() {
        LocalDateTime exactCutoff = LocalDateTime.of(2026, 11, 10, 14, 0, 0);
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(BigDecimal.ONE)
                .lengthCm(BigDecimal.TEN)
                .widthCm(BigDecimal.TEN)
                .heightCm(BigDecimal.TEN)
                .distanceKm(new BigDecimal("5.00"))
                .originZone("ZONE_1")
                .destinationZone("ZONE_1")
                .requestTime(exactCutoff)
                .build();

        assertDoesNotThrow(() -> strategy.calculate(context, sameDayRule));
    }

    @Test
    @DisplayName("Request time after cutoff (14:00:01) throws DeliveryRestrictionException")
    void testRequestAfterCutoffThrows() {
        LocalDateTime afterCutoff = LocalDateTime.of(2026, 11, 10, 14, 0, 1);
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(BigDecimal.ONE)
                .lengthCm(BigDecimal.TEN)
                .widthCm(BigDecimal.TEN)
                .heightCm(BigDecimal.TEN)
                .distanceKm(new BigDecimal("5.00"))
                .originZone("ZONE_1")
                .destinationZone("ZONE_1")
                .requestTime(afterCutoff)
                .build();

        DeliveryRestrictionException ex = assertThrows(DeliveryRestrictionException.class, () ->
                strategy.calculate(context, sameDayRule));
        assertTrue(ex.getMessage().contains("exceeds daily cutoff time"));
    }

    @Test
    @DisplayName("Injectable Clock provides deterministic evaluation when requestTime is not explicitly supplied")
    void testInjectableClockEvaluation() {
        // Fix clock at 14:30:00 (after 14:00 cutoff)
        Instant fixedInstant = LocalDateTime.of(2026, 11, 10, 14, 30, 0).atZone(zoneId).toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, zoneId);

        SameDayDeliveryPricingStrategy clockedStrategy = new SameDayDeliveryPricingStrategy(
                LocalTime.of(14, 0), BigDecimal.ONE, fixedClock
        );

        PricingContext contextWithoutTime = PricingContext.builder()
                .physicalWeightKg(BigDecimal.ONE)
                .lengthCm(BigDecimal.TEN)
                .widthCm(BigDecimal.TEN)
                .heightCm(BigDecimal.TEN)
                .distanceKm(new BigDecimal("5.00"))
                .originZone("ZONE_1")
                .destinationZone("ZONE_1")
                .requestTime(null) // Evaluate via clock
                .build();

        DeliveryRestrictionException ex = assertThrows(DeliveryRestrictionException.class, () ->
                clockedStrategy.calculate(contextWithoutTime, sameDayRule));
        assertTrue(ex.getMessage().contains("exceeds daily cutoff time"));
    }

    @Test
    @DisplayName("Intra-zone delivery with case-insensitive zone matching succeeds")
    void testIntraZoneCaseInsensitivePasses() {
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(BigDecimal.ONE)
                .lengthCm(BigDecimal.TEN)
                .widthCm(BigDecimal.TEN)
                .heightCm(BigDecimal.TEN)
                .distanceKm(new BigDecimal("5.00"))
                .originZone("colombo_central")
                .destinationZone("COLOMBO_CENTRAL")
                .requestTime(LocalDateTime.of(2026, 11, 10, 11, 0))
                .build();

        assertDoesNotThrow(() -> strategy.calculate(context, sameDayRule));
    }

    @Test
    @DisplayName("Cross-zone delivery throws DeliveryRestrictionException")
    void testCrossZoneThrows() {
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(BigDecimal.ONE)
                .lengthCm(BigDecimal.TEN)
                .widthCm(BigDecimal.TEN)
                .heightCm(BigDecimal.TEN)
                .distanceKm(new BigDecimal("15.00"))
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("KANDY_METRO")
                .requestTime(LocalDateTime.of(2026, 11, 10, 11, 0))
                .build();

        DeliveryRestrictionException ex = assertThrows(DeliveryRestrictionException.class, () ->
                strategy.calculate(context, sameDayRule));
        assertTrue(ex.getMessage().contains("restricted to intra-zone deliveries"));
    }

    @Test
    @DisplayName("Surge multiplier integrates with urgencyMultiplier preventing double multiplication")
    void testSurgePricingConsolidatedIntoSingleUrgencyMultiplier() {
        // Base urgency = 1.5000, Surge = 1.2500 -> Effective urgency = 1.5000 * 1.2500 = 1.8750
        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("1.00"))
                .lengthCm(new BigDecimal("10"))
                .widthCm(new BigDecimal("10"))
                .heightCm(new BigDecimal("10"))
                .distanceKm(new BigDecimal("10.00"))
                .originZone("ZONE_A")
                .destinationZone("ZONE_A")
                .requestTime(LocalDateTime.of(2026, 11, 10, 10, 0))
                .surgeMultiplier(new BigDecimal("1.2500"))
                .build();

        StrategyPricingResult result = strategy.calculate(context, sameDayRule);

        // Effective urgency multiplier = 1.5000 * 1.2500 = 1.8750
        assertEquals(0, new BigDecimal("1.8750").compareTo(result.getUrgencyMultiplier()),
                "Urgency multiplier and surge must be consolidated into a single effective multiplier");

        // Base fare = 700.00, Weight charge = 0.00, Distance charge = 10 * 50 = 500.00
        // Fare before multiplier = 700 + 500 = 1200.00
        // Subtotal = 1200.00 * 1.8750 = 2250.00
        assertEquals(0, new BigDecimal("2250.00").compareTo(result.getSubtotalFare()));
    }

    @Test
    @DisplayName("Configured default surge multiplier is used when context surge is absent or neutral")
    void testDefaultSurgeMultiplierUsed() {
        SameDayDeliveryPricingStrategy surgingStrategy = new SameDayDeliveryPricingStrategy(
                LocalTime.of(14, 0), new BigDecimal("1.2000"), Clock.systemDefaultZone()
        );

        PricingContext context = PricingContext.builder()
                .physicalWeightKg(new BigDecimal("1.00"))
                .lengthCm(BigDecimal.TEN)
                .widthCm(BigDecimal.TEN)
                .heightCm(BigDecimal.TEN)
                .distanceKm(new BigDecimal("10.00"))
                .originZone("ZONE_A")
                .destinationZone("ZONE_A")
                .requestTime(LocalDateTime.of(2026, 11, 10, 10, 0))
                .surgeMultiplier(BigDecimal.ONE) // Neutral surge in context
                .build();

        StrategyPricingResult result = surgingStrategy.calculate(context, sameDayRule);

        // Effective urgency = 1.5000 * 1.2000 = 1.8000
        assertEquals(0, new BigDecimal("1.8000").compareTo(result.getUrgencyMultiplier()));
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
                .originZone("Z1")
                .destinationZone("Z1")
                .build();

        assertThrows(IllegalArgumentException.class, () -> strategy.calculate(context, standardRule));
    }
}
