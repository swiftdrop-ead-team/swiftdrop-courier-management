package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.DeliveryZone;
import com.swiftdrop.pricing.entity.FuelSurchargeIndex;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.exception.FuelRateNotFoundException;
import com.swiftdrop.pricing.repository.FuelSurchargeIndexRepository;
import com.swiftdrop.pricing.strategy.StrategyPricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuelSurchargeServiceTest {

    @Mock
    private FuelSurchargeIndexRepository fuelSurchargeIndexRepository;

    private FuelSurchargeService fuelSurchargeService;
    private final ZoneId zoneId = ZoneId.of("Asia/Colombo");
    private final LocalDateTime fixedNow = LocalDateTime.of(2026, 11, 10, 10, 0, 0);

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(fixedNow.atZone(zoneId).toInstant(), zoneId);
        fuelSurchargeService = new FuelSurchargeService(fuelSurchargeIndexRepository, fixedClock);
    }

    @Test
    @DisplayName("Fuel surcharge calculates correctly on raw distance charge (Option 2A)")
    void testCalculateFuelSurchargeOnDistanceCharge() {
        BigDecimal distanceCharge = new BigDecimal("250.00");
        BigDecimal ratePercentage = new BigDecimal("5.00"); // 5%

        // 250.00 * (5.00 / 100) = 12.50
        BigDecimal surcharge = fuelSurchargeService.calculateFuelSurcharge(distanceCharge, ratePercentage);

        assertNotNull(surcharge);
        assertEquals(0, new BigDecimal("12.50").compareTo(surcharge));
    }

    @Test
    @DisplayName("Fuel surcharge applies HALF_UP rounding to 2 decimal places")
    void testCalculateFuelSurchargeRounding() {
        BigDecimal distanceCharge = new BigDecimal("175.50");
        BigDecimal ratePercentage = new BigDecimal("4.25"); // 4.25%

        // 175.50 * 0.0425 = 7.45875 -> rounds to 7.46
        BigDecimal surcharge = fuelSurchargeService.calculateFuelSurcharge(distanceCharge, ratePercentage);

        assertEquals(0, new BigDecimal("7.46").compareTo(surcharge));
    }

    @Test
    @DisplayName("Zero rate percentage produces 0.00 fuel surcharge")
    void testZeroRatePercentageProducesZeroSurcharge() {
        BigDecimal distanceCharge = new BigDecimal("350.00");
        BigDecimal ratePercentage = BigDecimal.ZERO;

        BigDecimal surcharge = fuelSurchargeService.calculateFuelSurcharge(distanceCharge, ratePercentage);

        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(surcharge));
    }

    @Test
    @DisplayName("Zero distance charge produces 0.00 fuel surcharge")
    void testZeroDistanceChargeProducesZeroSurcharge() {
        BigDecimal distanceCharge = BigDecimal.ZERO;
        BigDecimal ratePercentage = new BigDecimal("6.50");

        BigDecimal surcharge = fuelSurchargeService.calculateFuelSurcharge(distanceCharge, ratePercentage);

        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(surcharge));
    }

    @Test
    @DisplayName("getActiveFuelRate returns current active index from repository")
    void testGetActiveFuelRateSuccess() {
        FuelSurchargeIndex activeIndex = FuelSurchargeIndex.builder()
                .id(1L)
                .ratePercentage(new BigDecimal("4.75"))
                .effectiveFrom(fixedNow.minusDays(3))
                .isActive(true)
                .build();

        when(fuelSurchargeIndexRepository.findCurrentActive(fixedNow))
                .thenReturn(Optional.of(activeIndex));

        FuelSurchargeIndex result = fuelSurchargeService.getActiveFuelRate(fixedNow);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("4.75").compareTo(result.getRatePercentage()));
        verify(fuelSurchargeIndexRepository).findCurrentActive(fixedNow);
    }

    @Test
    @DisplayName("Missing active fuel rate strictly throws FuelRateNotFoundException")
    void testMissingFuelRateThrowsException() {
        when(fuelSurchargeIndexRepository.findCurrentActive(any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        FuelRateNotFoundException ex = assertThrows(FuelRateNotFoundException.class, () ->
                fuelSurchargeService.getActiveFuelRate(fixedNow));

        assertTrue(ex.getMessage().contains("No active fuel surcharge rate found effective on or before"));
    }

    @Test
    @DisplayName("Complete calculation pipeline executes in exact order without double multiplier application")
    void testCalculateZoneAndFuelPricingPipeline() {
        // Step 3 Strategy output:
        // baseFare: 350.00, weightCharge: 187.50, distanceCharge: 250.00, urgencyMultiplier: 1.0000
        // subtotalFare = (350 + 187.50 + 250) * 1.0 = 787.50
        StrategyPricingResult strategyResult = StrategyPricingResult.builder()
                .serviceType(ServiceType.STANDARD)
                .baseFare(new BigDecimal("350.00"))
                .weightCharge(new BigDecimal("187.50"))
                .distanceCharge(new BigDecimal("250.00"))
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .volumetricWeightKg(new BigDecimal("0.8000"))
                .billableWeightKg(new BigDecimal("3.50"))
                .extraWeightKg(new BigDecimal("2.50"))
                .subtotalFare(new BigDecimal("787.50"))
                .build();

        DeliveryZone origin = DeliveryZone.builder()
                .zoneCode("COLOMBO_CENTRAL")
                .baseZoneMultiplier(new BigDecimal("1.0000"))
                .isActive(true)
                .build();

        DeliveryZone destination = DeliveryZone.builder()
                .zoneCode("GREATER_COLOMBO")
                .baseZoneMultiplier(new BigDecimal("1.1500"))
                .isActive(true)
                .build();

        FuelSurchargeIndex activeIndex = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("5.00"))
                .effectiveFrom(fixedNow.minusDays(1))
                .isActive(true)
                .build();

        when(fuelSurchargeIndexRepository.findCurrentActive(fixedNow))
                .thenReturn(Optional.of(activeIndex));

        ZoneAndFuelPricingResult result = fuelSurchargeService.calculateZoneAndFuelPricing(
                strategyResult, origin, destination, fixedNow
        );

        assertNotNull(result);
        assertSame(origin, result.getOriginZone());
        assertSame(destination, result.getDestinationZone());

        // 1. Destination zone multiplier = 1.1500
        assertEquals(0, new BigDecimal("1.1500").compareTo(result.getZoneMultiplier()));

        // 2. Zone-adjusted fare = subtotalFare (787.50) * 1.1500 = 905.625 -> 905.63
        assertEquals(0, new BigDecimal("905.63").compareTo(result.getZoneAdjustedFare()));

        // 3. Fuel surcharge on raw distanceCharge: 250.00 * 0.05 = 12.50
        assertEquals(0, new BigDecimal("5.00").compareTo(result.getFuelSurchargeRatePercentage()));
        assertEquals(0, new BigDecimal("12.50").compareTo(result.getFuelSurchargeAmount()));

        // 4. Subtotal with fuel = 905.63 + 12.50 = 918.13
        assertEquals(0, new BigDecimal("918.13").compareTo(result.getSubtotalWithFuel()));
    }

    @Test
    @DisplayName("Negative distance charge throws IllegalArgumentException")
    void testNegativeDistanceChargeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                fuelSurchargeService.calculateFuelSurcharge(new BigDecimal("-10.00"), new BigDecimal("5.00")));
    }

    @Test
    @DisplayName("Negative rate percentage throws IllegalArgumentException")
    void testNegativeRatePercentageThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                fuelSurchargeService.calculateFuelSurcharge(new BigDecimal("100.00"), new BigDecimal("-5.00")));
    }

    @Test
    @DisplayName("Null arguments throw IllegalArgumentException")
    void testNullInputsThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                fuelSurchargeService.calculateFuelSurcharge(null, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () ->
                fuelSurchargeService.calculateFuelSurcharge(BigDecimal.TEN, null));
    }
}
