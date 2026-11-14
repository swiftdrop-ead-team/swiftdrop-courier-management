package com.swiftdrop.pricing.entity;

import com.swiftdrop.pricing.repository.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PricingEntitiesTest {

    @Autowired
    private DeliveryZoneRepository deliveryZoneRepository;

    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    @Autowired
    private MerchantTierDiscountRepository merchantTierDiscountRepository;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @Autowired
    private PromoRedemptionRepository promoRedemptionRepository;

    @Autowired
    private FuelSurchargeIndexRepository fuelSurchargeIndexRepository;

    @Autowired
    private PriceQuoteRepository priceQuoteRepository;

    @Autowired
    private Validator validator;

    @Test
    @DisplayName("DeliveryZone entity persists correctly and enforces unique zone_code")
    void testDeliveryZonePersistence() {
        DeliveryZone zone = DeliveryZone.builder()
                .zoneCode("COLOMBO_CENTRAL")
                .zoneName("Colombo Central Commercial Hub")
                .baseZoneMultiplier(new BigDecimal("1.0000"))
                .isActive(true)
                .build();

        DeliveryZone saved = deliveryZoneRepository.saveAndFlush(zone);
        assertNotNull(saved.getId());
        assertEquals("COLOMBO_CENTRAL", saved.getZoneCode());
        assertEquals(new BigDecimal("1.0000"), saved.getBaseZoneMultiplier());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        // Test unique constraint on zone_code
        DeliveryZone duplicate = DeliveryZone.builder()
                .zoneCode("COLOMBO_CENTRAL")
                .zoneName("Duplicate Zone")
                .baseZoneMultiplier(new BigDecimal("1.2000"))
                .isActive(true)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            deliveryZoneRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    @DisplayName("PricingRule entity persists with BigDecimal precision and ServiceType enum mapping")
    void testPricingRulePersistence() {
        PricingRule rule = PricingRule.builder()
                .serviceType(ServiceType.STANDARD)
                .baseFare(new BigDecimal("350.00"))
                .baseWeightKg(new BigDecimal("1.00"))
                .perKgRate(new BigDecimal("75.50"))
                .perKmRate(new BigDecimal("25.00"))
                .volumetricDivisor(5000)
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .isActive(true)
                .build();

        PricingRule saved = pricingRuleRepository.saveAndFlush(rule);
        assertNotNull(saved.getId());
        assertEquals(ServiceType.STANDARD, saved.getServiceType());
        assertEquals(0, new BigDecimal("350.00").compareTo(saved.getBaseFare()));
        assertEquals(0, new BigDecimal("75.50").compareTo(saved.getPerKgRate()));
        assertEquals(5000, saved.getVolumetricDivisor());

        Optional<PricingRule> retrieved = pricingRuleRepository.findByServiceTypeAndIsActiveTrue(ServiceType.STANDARD);
        assertTrue(retrieved.isPresent());
        assertEquals(saved.getId(), retrieved.get().getId());
    }

    @Test
    @DisplayName("MerchantTierDiscount entity persists with MerchantTier enum and discount percentage")
    void testMerchantTierDiscountPersistence() {
        MerchantTierDiscount discount = MerchantTierDiscount.builder()
                .tier(MerchantTier.GOLD)
                .discountPercentage(new BigDecimal("15.50"))
                .minMonthlyShipments(250)
                .isActive(true)
                .build();

        MerchantTierDiscount saved = merchantTierDiscountRepository.saveAndFlush(discount);
        assertNotNull(saved.getId());
        assertEquals(MerchantTier.GOLD, saved.getTier());
        assertEquals(0, new BigDecimal("15.50").compareTo(saved.getDiscountPercentage()));
        assertEquals(250, saved.getMinMonthlyShipments());

        Optional<MerchantTierDiscount> found = merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.GOLD);
        assertTrue(found.isPresent());
    }

    @Test
    @DisplayName("PromoCode and PromoRedemption relationship supports UUID quoteId, null quoteId, and idempotency")
    void testPromoCodeAndRedemptionRelationship() {
        // 1. Create and persist a valid promo code
        PromoCode promo = PromoCode.builder()
                .code("LAUNCH2026")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .minOrderAmount(new BigDecimal("500.00"))
                .maxDiscountCap(new BigDecimal("1000.00"))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .usageLimit(100)
                .timesUsed(0)
                .isActive(true)
                .build();
        PromoCode savedPromo = promoCodeRepository.saveAndFlush(promo);
        assertNotNull(savedPromo.getId());

        // 2. Create and persist a valid PriceQuote
        PriceQuote quote = PriceQuote.builder()
                .serviceType(ServiceType.STANDARD)
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("COLOMBO_CENTRAL")
                .physicalWeightKg(new BigDecimal("1.50"))
                .volumetricWeightKg(new BigDecimal("1.20"))
                .billableWeightKg(new BigDecimal("1.50"))
                .distanceKm(new BigDecimal("10.00"))
                .baseFare(new BigDecimal("350.00"))
                .weightCharge(new BigDecimal("50.00"))
                .distanceCharge(new BigDecimal("200.00"))
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .zoneMultiplier(new BigDecimal("1.0000"))
                .fuelSurchargeAmount(new BigDecimal("25.00"))
                .merchantDiscountAmount(BigDecimal.ZERO)
                .promoDiscountAmount(new BigDecimal("125.00"))
                .finalFare(new BigDecimal("500.00"))
                .currency("LKR")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        PriceQuote savedQuote = priceQuoteRepository.saveAndFlush(quote);
        assertNotNull(savedQuote.getId());

        // 3. Test redemption WITH valid quote UUID
        PromoRedemption redemptionWithQuote = PromoRedemption.builder()
                .promoCode(savedPromo)
                .promoCodeSnapshot("LAUNCH2026")
                .orderReference("ORD-PARCEL-2026-0001")
                .customerId("cust-user-101")
                .quoteId(savedQuote.getId())
                .discountAmount(new BigDecimal("125.00"))
                .orderAmount(new BigDecimal("625.00"))
                .build();

        PromoRedemption savedRedemption1 = promoRedemptionRepository.saveAndFlush(redemptionWithQuote);
        assertNotNull(savedRedemption1.getId());
        assertEquals("ORD-PARCEL-2026-0001", savedRedemption1.getOrderReference());
        assertEquals(savedQuote.getId(), savedRedemption1.getQuoteId());
        assertEquals("cust-user-101", savedRedemption1.getCustomerId());

        // 4. Test redemption WITHOUT quote (null quoteId supported for unquoted bookings)
        PromoRedemption redemptionWithoutQuote = PromoRedemption.builder()
                .promoCode(savedPromo)
                .promoCodeSnapshot("LAUNCH2026")
                .orderReference("ORD-PARCEL-2026-0002")
                .customerId("cust-user-102")
                .quoteId(null)
                .discountAmount(new BigDecimal("100.00"))
                .orderAmount(new BigDecimal("500.00"))
                .build();

        PromoRedemption savedRedemption2 = promoRedemptionRepository.saveAndFlush(redemptionWithoutQuote);
        assertNotNull(savedRedemption2.getId());
        assertNull(savedRedemption2.getQuoteId());

        // 5. Test duplicate order_reference rejection (idempotency guard)
        PromoRedemption duplicateRedemption = PromoRedemption.builder()
                .promoCode(savedPromo)
                .promoCodeSnapshot("LAUNCH2026")
                .orderReference("ORD-PARCEL-2026-0001")
                .customerId("cust-user-103")
                .quoteId(savedQuote.getId())
                .discountAmount(new BigDecimal("125.00"))
                .orderAmount(new BigDecimal("625.00"))
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            promoRedemptionRepository.saveAndFlush(duplicateRedemption);
        });
    }

    @Test
    @DisplayName("FuelSurchargeIndex entity persists and findFirstByIsActiveTrue / findLatestActive returns most recent active rate")
    @SuppressWarnings("deprecation") // Deliberately verify the legacy repository delegate remains compatible.
    void testFuelSurchargeIndexPersistence() {
        FuelSurchargeIndex older = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("3.50"))
                .effectiveFrom(LocalDateTime.now().minusDays(10))
                .isActive(true)
                .updatedBy("ADMIN-01")
                .build();
        fuelSurchargeIndexRepository.saveAndFlush(older);

        FuelSurchargeIndex newer = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("4.25"))
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .isActive(true)
                .updatedBy("ADMIN-02")
                .build();
        fuelSurchargeIndexRepository.saveAndFlush(newer);

        // Test standard Spring Data derived method
        Optional<FuelSurchargeIndex> latestDerived = fuelSurchargeIndexRepository.findFirstByIsActiveTrueOrderByEffectiveFromDescCreatedAtDesc();
        assertTrue(latestDerived.isPresent());
        assertEquals(0, new BigDecimal("4.25").compareTo(latestDerived.get().getRatePercentage()));

        // Test default interface delegate method
        Optional<FuelSurchargeIndex> latest = fuelSurchargeIndexRepository.findLatestActive();
        assertTrue(latest.isPresent());
        assertEquals(0, new BigDecimal("4.25").compareTo(latest.get().getRatePercentage()));
    }

    @Test
    @DisplayName("PriceQuote entity persists with UUID primary key and query by expiration works")
    void testPriceQuotePersistence() {
        PriceQuote quote = PriceQuote.builder()
                .serviceType(ServiceType.EXPRESS)
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("GREATER_COLOMBO")
                .physicalWeightKg(new BigDecimal("2.50"))
                .volumetricWeightKg(new BigDecimal("3.10"))
                .billableWeightKg(new BigDecimal("3.10"))
                .distanceKm(new BigDecimal("18.40"))
                .baseFare(new BigDecimal("500.00"))
                .weightCharge(new BigDecimal("157.50"))
                .distanceCharge(new BigDecimal("460.00"))
                .urgencyMultiplier(new BigDecimal("1.3500"))
                .zoneMultiplier(new BigDecimal("1.1500"))
                .fuelSurchargeAmount(new BigDecimal("48.50"))
                .merchantDiscountAmount(new BigDecimal("50.00"))
                .promoDiscountAmount(new BigDecimal("100.00"))
                .finalFare(new BigDecimal("1066.00"))
                .currency("LKR")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        PriceQuote saved = priceQuoteRepository.saveAndFlush(quote);
        assertNotNull(saved.getId());
        assertInstanceOf(UUID.class, saved.getId());
        assertEquals(0, new BigDecimal("1066.00").compareTo(saved.getFinalFare()));

        Optional<PriceQuote> activeQuote = priceQuoteRepository.findByIdAndExpiresAtAfter(saved.getId(), LocalDateTime.now());
        assertTrue(activeQuote.isPresent());

        Optional<PriceQuote> expiredQuery = priceQuoteRepository.findByIdAndExpiresAtAfter(saved.getId(), LocalDateTime.now().plusHours(1));
        assertFalse(expiredQuery.isPresent());
    }

    @Test
    @DisplayName("Validation constraints enforce positive and non-negative bounds across entities")
    void testEntityValidationConstraints() {
        // 1. Negative base fare on PricingRule
        PricingRule invalidRule = PricingRule.builder()
                .serviceType(ServiceType.SAME_DAY)
                .baseFare(new BigDecimal("-50.00"))
                .baseWeightKg(new BigDecimal("1.00"))
                .perKgRate(new BigDecimal("100.00"))
                .perKmRate(new BigDecimal("50.00"))
                .volumetricDivisor(5000)
                .urgencyMultiplier(new BigDecimal("1.5000"))
                .build();
        Set<ConstraintViolation<PricingRule>> ruleViolations = validator.validate(invalidRule);
        assertFalse(ruleViolations.isEmpty());
        assertTrue(ruleViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("baseFare")));

        // 2. Discount percentage > 100 on MerchantTierDiscount
        MerchantTierDiscount invalidTier = MerchantTierDiscount.builder()
                .tier(MerchantTier.PLATINUM)
                .discountPercentage(new BigDecimal("125.00"))
                .build();
        Set<ConstraintViolation<MerchantTierDiscount>> tierViolations = validator.validate(invalidTier);
        assertFalse(tierViolations.isEmpty());
        assertTrue(tierViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("discountPercentage")));

        // 3. Fuel surcharge percentage > 100
        FuelSurchargeIndex invalidSurcharge = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("105.00"))
                .build();
        Set<ConstraintViolation<FuelSurchargeIndex>> surchargeViolations = validator.validate(invalidSurcharge);
        assertFalse(surchargeViolations.isEmpty());
        assertTrue(surchargeViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("ratePercentage")));

        // 4. Zero/Negative multiplier on DeliveryZone
        DeliveryZone invalidZone = DeliveryZone.builder()
                .zoneCode("INVALID_ZONE")
                .zoneName("Invalid Zone")
                .baseZoneMultiplier(new BigDecimal("0.0000"))
                .build();
        Set<ConstraintViolation<DeliveryZone>> zoneViolations = validator.validate(invalidZone);
        assertFalse(zoneViolations.isEmpty());
        assertTrue(zoneViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("baseZoneMultiplier")));
    }
}
