package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.DeliveryZone;
import com.swiftdrop.pricing.entity.FuelSurchargeIndex;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.exception.FuelRateNotFoundException;
import com.swiftdrop.pricing.exception.InactiveZoneException;
import com.swiftdrop.pricing.exception.ZoneNotFoundException;
import com.swiftdrop.pricing.repository.DeliveryZoneRepository;
import com.swiftdrop.pricing.repository.FuelSurchargeIndexRepository;
import com.swiftdrop.pricing.strategy.StrategyPricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ZoneAndFuelIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DeliveryZoneRepository deliveryZoneRepository;

    @Autowired
    private FuelSurchargeIndexRepository fuelSurchargeIndexRepository;

    private DeliveryZoneService deliveryZoneService;
    private FuelSurchargeService fuelSurchargeService;

    @BeforeEach
    void setUp() {
        deliveryZoneService = new DeliveryZoneService(deliveryZoneRepository);
        fuelSurchargeService = new FuelSurchargeService(fuelSurchargeIndexRepository);
    }

    @Test
    @DisplayName("End-to-end integration: Database zone lookup, multiplier application, and distance-based fuel surcharge")
    void testEndToEndZoneAndFuelIntegration() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        DeliveryZone origin = DeliveryZone.builder()
                .zoneCode("COLOMBO_CENTRAL")
                .zoneName("Colombo Central")
                .baseZoneMultiplier(new BigDecimal("1.0000"))
                .isActive(true)
                .build();
        entityManager.persist(origin);

        DeliveryZone destination = DeliveryZone.builder()
                .zoneCode("GREATER_COLOMBO")
                .zoneName("Greater Colombo Suburbs")
                .baseZoneMultiplier(new BigDecimal("1.1500"))
                .isActive(true)
                .build();
        entityManager.persist(destination);

        FuelSurchargeIndex pastActiveIndex = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("5.50"))
                .effectiveFrom(now.minusDays(3))
                .isActive(true)
                .build();
        entityManager.persist(pastActiveIndex);

        // Future rate that must be excluded by findCurrentActive
        FuelSurchargeIndex futureIndex = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("8.00"))
                .effectiveFrom(now.plusDays(5))
                .isActive(true)
                .build();
        entityManager.persist(futureIndex);

        entityManager.flush();
        entityManager.clear();

        // 1. Case-insensitive zone lookup against real database
        DeliveryZone fetchedOrigin = deliveryZoneService.validateAndGetZone("colombo_central");
        DeliveryZone fetchedDestination = deliveryZoneService.validateAndGetZone("greater_colombo");
        assertNotNull(fetchedOrigin);
        assertNotNull(fetchedDestination);

        // 2. Simulated Step 3 Strategy result
        StrategyPricingResult strategyResult = StrategyPricingResult.builder()
                .serviceType(ServiceType.STANDARD)
                .baseFare(new BigDecimal("350.00"))
                .weightCharge(new BigDecimal("187.50"))
                .distanceCharge(new BigDecimal("200.00"))
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .volumetricWeightKg(new BigDecimal("0.8000"))
                .billableWeightKg(new BigDecimal("3.50"))
                .extraWeightKg(new BigDecimal("2.50"))
                .subtotalFare(new BigDecimal("737.50")) // (350 + 187.50 + 200) * 1.0 = 737.50
                .build();

        // 3. Complete Step 4 calculation
        ZoneAndFuelPricingResult result = fuelSurchargeService.calculateZoneAndFuelPricing(
                strategyResult, fetchedOrigin, fetchedDestination, now
        );

        assertNotNull(result);
        assertEquals(0, new BigDecimal("1.1500").compareTo(result.getZoneMultiplier()));

        // Zone-adjusted fare: 737.50 * 1.1500 = 848.125 -> 848.13
        assertEquals(0, new BigDecimal("848.13").compareTo(result.getZoneAdjustedFare()));

        // Fuel surcharge on raw distanceCharge (200.00): 200.00 * (5.50 / 100) = 11.00
        assertEquals(0, new BigDecimal("5.50").compareTo(result.getFuelSurchargeRatePercentage()));
        assertEquals(0, new BigDecimal("11.00").compareTo(result.getFuelSurchargeAmount()));

        // Subtotal with fuel: 848.13 + 11.00 = 859.13
        assertEquals(0, new BigDecimal("859.13").compareTo(result.getSubtotalWithFuel()));
    }

    @Test
    @DisplayName("Database zone validation distinguishes nonexistent from inactive zones")
    void testZoneLookupExceptionsInDatabase() {
        DeliveryZone inactive = DeliveryZone.builder()
                .zoneCode("INACTIVE_ZONE")
                .zoneName("Inactive Outpost")
                .baseZoneMultiplier(new BigDecimal("1.2000"))
                .isActive(false)
                .build();
        entityManager.persistAndFlush(inactive);
        entityManager.clear();

        // Nonexistent zone throws ZoneNotFoundException
        assertThrows(ZoneNotFoundException.class, () ->
                deliveryZoneService.validateAndGetZone("COMPLETELY_UNKNOWN"));

        // Existing inactive zone throws InactiveZoneException
        assertThrows(InactiveZoneException.class, () ->
                deliveryZoneService.validateAndGetZone("INACTIVE_ZONE"));
    }

    @Test
    @DisplayName("Missing active fuel rate in database throws FuelRateNotFoundException")
    void testFuelRateNotFoundInDatabase() {
        LocalDateTime now = LocalDateTime.now();

        // Persist only future rate
        FuelSurchargeIndex futureRate = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("7.00"))
                .effectiveFrom(now.plusDays(10))
                .isActive(true)
                .build();
        entityManager.persistAndFlush(futureRate);
        entityManager.clear();

        assertThrows(FuelRateNotFoundException.class, () ->
                fuelSurchargeService.getActiveFuelRate(now));
    }
}
