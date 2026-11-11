package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.DiscountType;
import com.swiftdrop.pricing.entity.PromoCode;
import com.swiftdrop.pricing.exception.InvalidPromoCodeException;
import com.swiftdrop.pricing.repository.PromoCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    private PromoCodeService promoCodeService;

    private final LocalDateTime fixedNow = LocalDateTime.of(2026, 11, 10, 12, 0, 0);
    private final ZoneId zoneId = ZoneId.of("Asia/Colombo");

    @BeforeEach
    void setUp() {
        Instant fixedInstant = fixedNow.atZone(zoneId).toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, zoneId);
        promoCodeService = new PromoCodeService(promoCodeRepository, fixedClock);
    }

    private PromoCode createValidPromo(DiscountType discountType, BigDecimal discountValue) {
        return PromoCode.builder()
                .id(1L)
                .code("SWIFT2026")
                .discountType(discountType)
                .discountValue(discountValue)
                .minOrderAmount(new BigDecimal("100.00"))
                .validFrom(fixedNow.minusDays(5))
                .validTo(fixedNow.plusDays(5))
                .usageLimit(100)
                .timesUsed(10)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("Valid Promo Code Discount Calculations")
    class ValidPromoCalculations {

        @Test
        @DisplayName("Percentage discount calculates correctly without cap")
        void calculatePercentage_withoutCap_returnsExpectedDiscount() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("20.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal fare = new BigDecimal("1000.00");
            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", fare);

            assertEquals(new BigDecimal("200.00"), discount);
            assertEquals(2, discount.scale());
        }

        @Test
        @DisplayName("Percentage discount capped by maxDiscountCap")
        void calculatePercentage_withCap_cappedCorrectly() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("20.00"));
            promo.setMaxDiscountCap(new BigDecimal("250.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal fare = new BigDecimal("2000.00"); // 20% of 2000 = 400 > cap 250
            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", fare);

            assertEquals(new BigDecimal("250.00"), discount);
            assertEquals(2, discount.scale());
        }

        @Test
        @DisplayName("Flat amount discount below fare returns exact flat value")
        void calculateFlat_belowFare_returnsFlatValue() {
            PromoCode promo = createValidPromo(DiscountType.FLAT_AMOUNT, new BigDecimal("150.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal fare = new BigDecimal("500.00");
            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", fare);

            assertEquals(new BigDecimal("150.00"), discount);
            assertEquals(2, discount.scale());
        }

        @Test
        @DisplayName("Flat amount discount exceeding fare is capped at zoneAdjustedFare")
        void calculateFlat_exceedingFare_cappedAtFare() {
            PromoCode promo = createValidPromo(DiscountType.FLAT_AMOUNT, new BigDecimal("200.00"));
            promo.setMinOrderAmount(BigDecimal.ZERO);
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal fare = new BigDecimal("120.00");
            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", fare);

            assertEquals(new BigDecimal("120.00"), discount);
            assertEquals(2, discount.scale());
        }

        @Test
        @DisplayName("Case-insensitive lookup matches successfully (e.g. 'swift2026' matches 'SWIFT2026')")
        void calculateDiscount_caseInsensitiveMatch_succeeds() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("swift2026")).thenReturn(List.of(promo));

            BigDecimal fare = new BigDecimal("500.00");
            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("  swift2026  ", fare);

            assertEquals(new BigDecimal("50.00"), discount);
            verify(promoCodeRepository).findByCodeIgnoreCase("swift2026");
        }

        @Test
        @DisplayName("Percentage discount rounding uses HALF_UP scale 2")
        void calculatePercentage_rounding_halfUp() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("7.55"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal fare = new BigDecimal("125.55"); // 125.55 * 0.0755 = 9.479025 -> 9.48
            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", fare);

            assertEquals(new BigDecimal("9.48"), discount);
            assertEquals(2, discount.scale());
        }
    }

    @Nested
    @DisplayName("Ambiguity and Code Lookup Validation")
    class AmbiguityAndLookup {

        @Test
        @DisplayName("Nonexistent promo code throws InvalidPromoCodeException")
        void lookup_nonexistentPromo_throwsException() {
            when(promoCodeRepository.findByCodeIgnoreCase("UNKNOWN")).thenReturn(Collections.emptyList());

            InvalidPromoCodeException ex = assertThrows(InvalidPromoCodeException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("UNKNOWN", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("Promo code not found"));
        }

        @Test
        @DisplayName("Multiple case-variant matches throw InvalidPromoCodeException stating ambiguity")
        void lookup_multipleMatches_throwsAmbiguityException() {
            PromoCode promo1 = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo1.setCode("PROMO10");

            PromoCode promo2 = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("15.00"));
            promo2.setCode("promo10");

            when(promoCodeRepository.findByCodeIgnoreCase("promo10")).thenReturn(List.of(promo1, promo2));

            InvalidPromoCodeException ex = assertThrows(InvalidPromoCodeException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("promo10", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("Ambiguous promo code"));
            assertTrue(ex.getMessage().contains("multiple case-variant records"));
        }

        @Test
        @DisplayName("Null or blank promo code returns 0.00 without querying repository")
        void lookup_nullOrBlank_returnsZero() {
            BigDecimal fare = new BigDecimal("500.00");

            assertEquals(new BigDecimal("0.00"), promoCodeService.validateAndCalculateDiscount(null, fare));
            assertEquals(new BigDecimal("0.00"), promoCodeService.validateAndCalculateDiscount("", fare));
            assertEquals(new BigDecimal("0.00"), promoCodeService.validateAndCalculateDiscount("   ", fare));

            verifyNoInteractions(promoCodeRepository);
        }
    }

    @Nested
    @DisplayName("Validity Dates and Active Status Boundaries")
    class ValidityAndStatus {

        @Test
        @DisplayName("Inactive promo code throws InvalidPromoCodeException")
        void validity_inactive_throwsException() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setIsActive(false);
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            InvalidPromoCodeException ex = assertThrows(InvalidPromoCodeException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("inactive"));
        }

        @Test
        @DisplayName("Boundary: Evaluation exactly at validFrom timestamp is accepted")
        void validity_exactlyAtValidFrom_accepted() {
            LocalDateTime validFrom = fixedNow;
            LocalDateTime validTo = fixedNow.plusDays(10);
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setValidFrom(validFrom);
            promo.setValidTo(validTo);

            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"), validFrom);
            assertEquals(new BigDecimal("50.00"), discount);
        }

        @Test
        @DisplayName("Boundary: Evaluation exactly at validTo timestamp is accepted")
        void validity_exactlyAtValidTo_accepted() {
            LocalDateTime validFrom = fixedNow.minusDays(10);
            LocalDateTime validTo = fixedNow;
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setValidFrom(validFrom);
            promo.setValidTo(validTo);

            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"), validTo);
            assertEquals(new BigDecimal("50.00"), discount);
        }

        @Test
        @DisplayName("Boundary: Evaluation immediately before validFrom throws InvalidPromoCodeException")
        void validity_beforeValidFrom_throwsException() {
            LocalDateTime validFrom = fixedNow.plusSeconds(1);
            LocalDateTime validTo = fixedNow.plusDays(10);
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setValidFrom(validFrom);
            promo.setValidTo(validTo);

            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            InvalidPromoCodeException ex = assertThrows(InvalidPromoCodeException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"), fixedNow));

            assertTrue(ex.getMessage().contains("expired or not yet valid"));
        }

        @Test
        @DisplayName("Boundary: Evaluation immediately after validTo throws InvalidPromoCodeException")
        void validity_afterValidTo_throwsException() {
            LocalDateTime validFrom = fixedNow.minusDays(10);
            LocalDateTime validTo = fixedNow.minusSeconds(1);
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setValidFrom(validFrom);
            promo.setValidTo(validTo);

            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            InvalidPromoCodeException ex = assertThrows(InvalidPromoCodeException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"), fixedNow));

            assertTrue(ex.getMessage().contains("expired or not yet valid"));
        }
    }

    @Nested
    @DisplayName("Usage Limit and Minimum Order Amount Boundaries")
    class UsageLimitAndMinOrder {

        @Test
        @DisplayName("Boundary: Fare exactly equal to minOrderAmount is accepted")
        void minOrder_exactlyEqual_accepted() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setMinOrderAmount(new BigDecimal("500.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"));
            assertEquals(new BigDecimal("50.00"), discount);
        }

        @Test
        @DisplayName("Fare strictly below minOrderAmount throws InvalidPromoCodeException")
        void minOrder_below_throwsException() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setMinOrderAmount(new BigDecimal("500.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal fare = new BigDecimal("499.99");
            InvalidPromoCodeException ex = assertThrows(InvalidPromoCodeException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", fare));

            assertTrue(ex.getMessage().contains("does not meet minimum required amount"));
        }

        @Test
        @DisplayName("Boundary: timesUsed exactly equal to usageLimit throws InvalidPromoCodeException")
        void usageLimit_exactlyEqual_throwsException() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setUsageLimit(50);
            promo.setTimesUsed(50);
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            InvalidPromoCodeException ex = assertThrows(InvalidPromoCodeException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("usage limit has been reached"));
        }

        @Test
        @DisplayName("timesUsed strictly less than usageLimit is accepted")
        void usageLimit_lessThan_accepted() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setUsageLimit(50);
            promo.setTimesUsed(49);
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"));
            assertEquals(new BigDecimal("50.00"), discount);
        }

        @Test
        @DisplayName("Null usageLimit allows unlimited redemptions")
        void usageLimit_null_allowsUnlimited() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setUsageLimit(null);
            promo.setTimesUsed(100000);
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"));
            assertEquals(new BigDecimal("50.00"), discount);
        }
    }

    @Nested
    @DisplayName("Input Validation and Defensive Guards")
    class InputValidationAndDefenses {

        @Test
        @DisplayName("Null zoneAdjustedFare throws IllegalArgumentException")
        void input_nullFare_throwsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", null));

            assertEquals("Zone-adjusted fare cannot be null", ex.getMessage());
            verifyNoInteractions(promoCodeRepository);
        }

        @Test
        @DisplayName("Negative zoneAdjustedFare throws IllegalArgumentException")
        void input_negativeFare_throwsException() {
            BigDecimal negativeFare = new BigDecimal("-1.00");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", negativeFare));

            assertEquals("Zone-adjusted fare cannot be negative", ex.getMessage());
            verifyNoInteractions(promoCodeRepository);
        }

        @Test
        @DisplayName("Null evaluationTime in 3-argument method throws IllegalArgumentException")
        void input_nullEvaluationTime_throwsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"), null));

            assertEquals("Evaluation time cannot be null", ex.getMessage());
            verifyNoInteractions(promoCodeRepository);
        }

        @Test
        @DisplayName("Two-argument overload uses injected Clock when promo is active and current")
        void defenses_twoArgOverload_usesInjectedClock() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"));
            assertEquals(new BigDecimal("50.00"), discount);
        }

        @Test
        @DisplayName("Two-argument overload fails when injected Clock indicates expiration")
        void defenses_twoArgOverload_clockExpired_throwsException() {
            Instant futureInstant = fixedNow.plusDays(20).atZone(zoneId).toInstant();
            Clock futureClock = Clock.fixed(futureInstant, zoneId);
            PromoCodeService serviceWithFutureClock = new PromoCodeService(promoCodeRepository, futureClock);

            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            InvalidPromoCodeException ex = assertThrows(InvalidPromoCodeException.class, () ->
                    serviceWithFutureClock.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("expired or not yet valid"));
        }

        @Test
        @DisplayName("Null timesUsed is safely treated as 0 without throwing NullPointerException")
        void defenses_nullTimesUsed_treatedAsZero() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setUsageLimit(10);
            promo.setTimesUsed(null);
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"));
            assertEquals(new BigDecimal("50.00"), discount);
        }

        @Test
        @DisplayName("Quotation is non-destructive: repository save is never invoked and timesUsed is not incremented")
        void defenses_nonDestructive_timesUsedNotMutated() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("10.00"));
            promo.setTimesUsed(15);
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"));

            assertEquals(15, promo.getTimesUsed(), "timesUsed must not be modified during quotation");
            verify(promoCodeRepository, never()).save(any());
            verify(promoCodeRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Invalid Stored Promo Configuration Defense")
    class InvalidConfigurationDefense {

        @Test
        @DisplayName("Null discountValue throws IllegalStateException")
        void config_nullDiscountValue_throwsException() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, null);
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("discount value must be strictly positive"));
        }

        @Test
        @DisplayName("Zero discountValue throws IllegalStateException")
        void config_zeroDiscountValue_throwsException() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, BigDecimal.ZERO);
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("discount value must be strictly positive"));
        }

        @Test
        @DisplayName("Negative discountValue throws IllegalStateException")
        void config_negativeDiscountValue_throwsException() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("-10.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("discount value must be strictly positive"));
        }

        @Test
        @DisplayName("Percentage discountValue above 100 throws IllegalStateException")
        void config_percentageAbove100_throwsException() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("100.01"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("percentage discount cannot exceed 100"));
        }

        @Test
        @DisplayName("Boundary: Percentage discountValue exactly 100 is accepted")
        void config_percentageExactly100_accepted() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("100.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            BigDecimal discount = promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00"));
            assertEquals(new BigDecimal("500.00"), discount);
        }

        @Test
        @DisplayName("Negative maxDiscountCap throws IllegalStateException")
        void config_negativeMaxDiscountCap_throwsException() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("20.00"));
            promo.setMaxDiscountCap(new BigDecimal("-1.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("max discount cap cannot be negative"));
        }

        @Test
        @DisplayName("Null discountType throws IllegalStateException")
        void config_nullDiscountType_throwsException() {
            PromoCode promo = createValidPromo(null, new BigDecimal("20.00"));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("discount type cannot be null"));
        }

        @Test
        @DisplayName("Null validFrom throws IllegalStateException without throwing NullPointerException")
        void config_nullValidFrom_throwsException() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("20.00"));
            promo.setValidFrom(null);
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("validity dates cannot be null"));
        }

        @Test
        @DisplayName("Null validTo throws IllegalStateException without throwing NullPointerException")
        void config_nullValidTo_throwsException() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("20.00"));
            promo.setValidTo(null);
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("validity dates cannot be null"));
        }

        @Test
        @DisplayName("validTo before validFrom throws IllegalStateException")
        void config_validToBeforeValidFrom_throwsException() {
            PromoCode promo = createValidPromo(DiscountType.PERCENTAGE, new BigDecimal("20.00"));
            promo.setValidFrom(fixedNow.plusDays(5));
            promo.setValidTo(fixedNow.minusDays(5));
            when(promoCodeRepository.findByCodeIgnoreCase("SWIFT2026")).thenReturn(List.of(promo));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    promoCodeService.validateAndCalculateDiscount("SWIFT2026", new BigDecimal("500.00")));

            assertTrue(ex.getMessage().contains("validTo cannot be before validFrom"));
        }
    }
}
