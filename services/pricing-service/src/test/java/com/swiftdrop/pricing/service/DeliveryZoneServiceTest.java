package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.DeliveryZone;
import com.swiftdrop.pricing.exception.InactiveZoneException;
import com.swiftdrop.pricing.exception.ZoneNotFoundException;
import com.swiftdrop.pricing.repository.DeliveryZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryZoneServiceTest {

    @Mock
    private DeliveryZoneRepository deliveryZoneRepository;

    @InjectMocks
    private DeliveryZoneService deliveryZoneService;

    private DeliveryZone colomboCentral;
    private DeliveryZone greaterColombo;
    private DeliveryZone inactiveZone;

    @BeforeEach
    void setUp() {
        colomboCentral = DeliveryZone.builder()
                .id(1L)
                .zoneCode("COLOMBO_CENTRAL")
                .zoneName("Colombo Central")
                .baseZoneMultiplier(new BigDecimal("1.0000"))
                .isActive(true)
                .build();

        greaterColombo = DeliveryZone.builder()
                .id(2L)
                .zoneCode("GREATER_COLOMBO")
                .zoneName("Greater Colombo Suburbs")
                .baseZoneMultiplier(new BigDecimal("1.1500"))
                .isActive(true)
                .build();

        inactiveZone = DeliveryZone.builder()
                .id(3L)
                .zoneCode("RESTRICTED_ZONE")
                .zoneName("Restricted Outpost")
                .baseZoneMultiplier(new BigDecimal("1.5000"))
                .isActive(false)
                .build();
    }

    @Test
    @DisplayName("Valid active zone code returns DeliveryZone entity (case-insensitive)")
    void testValidateAndGetZoneSuccessCaseInsensitive() {
        when(deliveryZoneRepository.findByZoneCodeIgnoreCase("colombo_central"))
                .thenReturn(Optional.of(colomboCentral));

        DeliveryZone result = deliveryZoneService.validateAndGetZone("colombo_central");

        assertNotNull(result);
        assertEquals("COLOMBO_CENTRAL", result.getZoneCode());
        assertEquals(0, new BigDecimal("1.0000").compareTo(result.getBaseZoneMultiplier()));
        verify(deliveryZoneRepository).findByZoneCodeIgnoreCase("colombo_central");
    }

    @Test
    @DisplayName("Nonexistent zone code throws ZoneNotFoundException")
    void testValidateAndGetZoneNotFoundThrows() {
        when(deliveryZoneRepository.findByZoneCodeIgnoreCase("UNKNOWN_ZONE"))
                .thenReturn(Optional.empty());

        ZoneNotFoundException ex = assertThrows(ZoneNotFoundException.class, () ->
                deliveryZoneService.validateAndGetZone("UNKNOWN_ZONE"));

        assertTrue(ex.getMessage().contains("Delivery zone not found with code: UNKNOWN_ZONE"));
    }

    @Test
    @DisplayName("Existing but inactive zone throws InactiveZoneException")
    void testValidateAndGetZoneInactiveThrows() {
        when(deliveryZoneRepository.findByZoneCodeIgnoreCase("RESTRICTED_ZONE"))
                .thenReturn(Optional.of(inactiveZone));

        InactiveZoneException ex = assertThrows(InactiveZoneException.class, () ->
                deliveryZoneService.validateAndGetZone("RESTRICTED_ZONE"));

        assertTrue(ex.getMessage().contains("Delivery zone is inactive: RESTRICTED_ZONE"));
    }

    @Test
    @DisplayName("Null or empty zone code throws ZoneNotFoundException")
    void testValidateAndGetZoneNullOrEmptyThrows() {
        assertThrows(ZoneNotFoundException.class, () -> deliveryZoneService.validateAndGetZone(null));
        assertThrows(ZoneNotFoundException.class, () -> deliveryZoneService.validateAndGetZone("   "));
        verifyNoInteractions(deliveryZoneRepository);
    }

    @Test
    @DisplayName("Inter-zone multiplier resolves to destination zone multiplier (Option 1)")
    void testResolveZoneMultiplierInterZone() {
        BigDecimal multiplier = deliveryZoneService.resolveZoneMultiplier(colomboCentral, greaterColombo);

        assertNotNull(multiplier);
        assertEquals(0, new BigDecimal("1.1500").compareTo(multiplier),
                "Destination zone multiplier must be used for inter-zone deliveries");
    }

    @Test
    @DisplayName("Intra-zone multiplier resolves to destination zone multiplier")
    void testResolveZoneMultiplierIntraZone() {
        BigDecimal multiplier = deliveryZoneService.resolveZoneMultiplier(colomboCentral, colomboCentral);

        assertNotNull(multiplier);
        assertEquals(0, new BigDecimal("1.0000").compareTo(multiplier));
    }

    @Test
    @DisplayName("applyZoneMultiplier calculates subtotal * zoneMultiplier with HALF_UP rounding")
    void testApplyZoneMultiplierCalculation() {
        BigDecimal subtotal = new BigDecimal("787.50");
        BigDecimal multiplier = new BigDecimal("1.1500");

        // 787.50 * 1.1500 = 905.625 -> rounds to 905.63
        BigDecimal zoneAdjusted = deliveryZoneService.applyZoneMultiplier(subtotal, multiplier);

        assertEquals(0, new BigDecimal("905.63").compareTo(zoneAdjusted));
    }

    @Test
    @DisplayName("applyZoneMultiplier with multiplier 1.0000 preserves subtotal")
    void testApplyZoneMultiplierNeutral() {
        BigDecimal subtotal = new BigDecimal("500.00");
        BigDecimal multiplier = new BigDecimal("1.0000");

        BigDecimal zoneAdjusted = deliveryZoneService.applyZoneMultiplier(subtotal, multiplier);

        assertEquals(0, new BigDecimal("500.00").compareTo(zoneAdjusted));
    }

    @Test
    @DisplayName("applyZoneMultiplier throws on null inputs")
    void testApplyZoneMultiplierNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                deliveryZoneService.applyZoneMultiplier(null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () ->
                deliveryZoneService.applyZoneMultiplier(BigDecimal.TEN, null));
    }
}
