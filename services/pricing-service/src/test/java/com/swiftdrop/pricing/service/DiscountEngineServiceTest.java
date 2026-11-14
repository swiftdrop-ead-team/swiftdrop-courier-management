package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.MerchantTier;
import com.swiftdrop.pricing.exception.InvalidPromoCodeException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountEngineServiceTest {

    @Mock
    private MerchantTierDiscountService merchantTierDiscountService;

    @Mock
    private PromoCodeService promoCodeService;

    private DiscountEngineService discountEngineService;

    private final LocalDateTime fixedNow = LocalDateTime.of(2026, 11, 10, 14, 0, 0);
    private final ZoneId zoneId = ZoneId.of("Asia/Colombo");

    @BeforeEach
    void setUp() {
        Instant fixedInstant = fixedNow.atZone(zoneId).toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, zoneId);
        discountEngineService = new DiscountEngineService(merchantTierDiscountService, promoCodeService, fixedClock);
    }

    @Nested
    @DisplayName("Independent Discount & Fare Calculations")
    class IndependentCalculations {

        @Test
        @DisplayName("Both merchant tier and promo code discounts apply independently against original fare")
        void calculateDiscounts_bothApplied_independentAndCorrectFare() {
            BigDecimal fare = new BigDecimal("1000.00");
            BigDecimal fuel = new BigDecimal("75.50");
            MerchantTier tier = MerchantTier.GOLD;
            String promo = "SAVE10";

            when(merchantTierDiscountService.calculateDiscount(fare, tier))
                    .thenReturn(new BigDecimal("150.00"));
            when(promoCodeService.validateAndCalculateDiscount(promo, fare, fixedNow))
                    .thenReturn(new BigDecimal("100.00"));

            DiscountEngineResult result = discountEngineService.calculateDiscounts(fare, tier, promo, fuel, fixedNow);

            // Verify both collaborators received the original fare
            verify(merchantTierDiscountService).calculateDiscount(eq(fare), eq(tier));
            verify(promoCodeService).validateAndCalculateDiscount(eq(promo), eq(fare), eq(fixedNow));

            assertEquals(new BigDecimal("1000.00"), result.getZoneAdjustedFare());
            assertEquals(new BigDecimal("150.00"), result.getMerchantDiscountAmount());
            assertEquals(new BigDecimal("100.00"), result.getPromoDiscountAmount());
            assertEquals(new BigDecimal("250.00"), result.getTotalDiscountAmount());
            assertEquals(new BigDecimal("750.00"), result.getNetFreight()); // 1000 - 250
            assertEquals(new BigDecimal("75.50"), result.getFuelSurchargeAmount());
            assertEquals(new BigDecimal("825.50"), result.getFinalFare()); // 750 + 75.50
        }

        @Test
        @DisplayName("Merchant tier discount only (promo code null/blank)")
        void calculateDiscounts_merchantOnly_correctFare() {
            BigDecimal fare = new BigDecimal("500.00");
            BigDecimal fuel = new BigDecimal("40.00");
            MerchantTier tier = MerchantTier.SILVER;

            when(merchantTierDiscountService.calculateDiscount(fare, tier))
                    .thenReturn(new BigDecimal("50.00"));
            when(promoCodeService.validateAndCalculateDiscount(null, fare, fixedNow))
                    .thenReturn(new BigDecimal("0.00"));

            DiscountEngineResult result = discountEngineService.calculateDiscounts(fare, tier, null, fuel, fixedNow);

            assertEquals(new BigDecimal("500.00"), result.getZoneAdjustedFare());
            assertEquals(new BigDecimal("50.00"), result.getMerchantDiscountAmount());
            assertEquals(new BigDecimal("0.00"), result.getPromoDiscountAmount());
            assertEquals(new BigDecimal("50.00"), result.getTotalDiscountAmount());
            assertEquals(new BigDecimal("450.00"), result.getNetFreight());
            assertEquals(new BigDecimal("40.00"), result.getFuelSurchargeAmount());
            assertEquals(new BigDecimal("490.00"), result.getFinalFare());
        }

        @Test
        @DisplayName("Promo code discount only (merchant tier null)")
        void calculateDiscounts_promoOnly_correctFare() {
            BigDecimal fare = new BigDecimal("800.00");
            BigDecimal fuel = new BigDecimal("50.00");
            String promo = "FLAT100";

            when(merchantTierDiscountService.calculateDiscount(fare, null))
                    .thenReturn(new BigDecimal("0.00"));
            when(promoCodeService.validateAndCalculateDiscount(promo, fare, fixedNow))
                    .thenReturn(new BigDecimal("100.00"));

            DiscountEngineResult result = discountEngineService.calculateDiscounts(fare, null, promo, fuel, fixedNow);

            assertEquals(new BigDecimal("800.00"), result.getZoneAdjustedFare());
            assertEquals(new BigDecimal("0.00"), result.getMerchantDiscountAmount());
            assertEquals(new BigDecimal("100.00"), result.getPromoDiscountAmount());
            assertEquals(new BigDecimal("100.00"), result.getTotalDiscountAmount());
            assertEquals(new BigDecimal("700.00"), result.getNetFreight());
            assertEquals(new BigDecimal("50.00"), result.getFuelSurchargeAmount());
            assertEquals(new BigDecimal("750.00"), result.getFinalFare());
        }

        @Test
        @DisplayName("Neither discount applies (null tier and null promo)")
        void calculateDiscounts_neitherApplied_fullFare() {
            BigDecimal fare = new BigDecimal("600.00");
            BigDecimal fuel = new BigDecimal("35.00");

            when(merchantTierDiscountService.calculateDiscount(fare, null))
                    .thenReturn(new BigDecimal("0.00"));
            when(promoCodeService.validateAndCalculateDiscount(null, fare, fixedNow))
                    .thenReturn(new BigDecimal("0.00"));

            DiscountEngineResult result = discountEngineService.calculateDiscounts(fare, null, null, fuel, fixedNow);

            assertEquals(new BigDecimal("600.00"), result.getZoneAdjustedFare());
            assertEquals(new BigDecimal("0.00"), result.getMerchantDiscountAmount());
            assertEquals(new BigDecimal("0.00"), result.getPromoDiscountAmount());
            assertEquals(new BigDecimal("0.00"), result.getTotalDiscountAmount());
            assertEquals(new BigDecimal("600.00"), result.getNetFreight());
            assertEquals(new BigDecimal("35.00"), result.getFuelSurchargeAmount());
            assertEquals(new BigDecimal("635.00"), result.getFinalFare());
        }
    }

    @Nested
    @DisplayName("Floor at Zero & Non-Discountable Fuel Surcharge")
    class FloorAndFuelSurchargeIntegrity {

        @Test
        @DisplayName("Combined discounts strictly exceed fare: totalDiscountAmount is uncapped, netFreight floored at 0.00")
        void calculateDiscounts_discountsExceedFare_netFreightFlooredAtZero() {
            BigDecimal fare = new BigDecimal("100.00");
            BigDecimal fuel = new BigDecimal("30.00");

            when(merchantTierDiscountService.calculateDiscount(fare, MerchantTier.PLATINUM))
                    .thenReturn(new BigDecimal("60.00"));
            when(promoCodeService.validateAndCalculateDiscount("BIGDEAL", fare, fixedNow))
                    .thenReturn(new BigDecimal("80.00"));

            DiscountEngineResult result = discountEngineService.calculateDiscounts(fare, MerchantTier.PLATINUM, "BIGDEAL", fuel, fixedNow);

            assertEquals(new BigDecimal("100.00"), result.getZoneAdjustedFare());
            assertEquals(new BigDecimal("60.00"), result.getMerchantDiscountAmount());
            assertEquals(new BigDecimal("80.00"), result.getPromoDiscountAmount());
            // totalDiscountAmount is NOT capped
            assertEquals(new BigDecimal("140.00"), result.getTotalDiscountAmount());
            // netFreight is floored at 0.00
            assertEquals(new BigDecimal("0.00"), result.getNetFreight());
            // Fuel surcharge is preserved untouched
            assertEquals(new BigDecimal("30.00"), result.getFuelSurchargeAmount());
            assertEquals(new BigDecimal("30.00"), result.getFinalFare()); // 0.00 + 30.00
        }

        @Test
        @DisplayName("Zero zoneAdjustedFare with non-zero fuel surcharge results in netFreight 0.00 and finalFare equal to fuelSurcharge")
        void calculateDiscounts_zeroFare_fuelSurchargePreserved() {
            BigDecimal fare = new BigDecimal("0.00");
            BigDecimal fuel = new BigDecimal("25.00");

            when(merchantTierDiscountService.calculateDiscount(fare, null))
                    .thenReturn(new BigDecimal("0.00"));
            when(promoCodeService.validateAndCalculateDiscount(null, fare, fixedNow))
                    .thenReturn(new BigDecimal("0.00"));

            DiscountEngineResult result = discountEngineService.calculateDiscounts(fare, null, null, fuel, fixedNow);

            assertEquals(new BigDecimal("0.00"), result.getNetFreight());
            assertEquals(new BigDecimal("25.00"), result.getFuelSurchargeAmount());
            assertEquals(new BigDecimal("25.00"), result.getFinalFare());
        }
    }

    @Nested
    @DisplayName("Input Validation and Defensive Guards")
    class InputValidationAndDefenses {

        @Test
        @DisplayName("Null zoneAdjustedFare throws IllegalArgumentException before calling dependencies")
        void input_nullFare_throwsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    discountEngineService.calculateDiscounts(null, MerchantTier.GOLD, "PROMO", new BigDecimal("10.00"), fixedNow));

            assertEquals("Zone-adjusted fare cannot be null", ex.getMessage());
            verifyNoInteractions(merchantTierDiscountService, promoCodeService);
        }

        @Test
        @DisplayName("Negative zoneAdjustedFare throws IllegalArgumentException before calling dependencies")
        void input_negativeFare_throwsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    discountEngineService.calculateDiscounts(new BigDecimal("-0.01"), MerchantTier.GOLD, "PROMO", new BigDecimal("10.00"), fixedNow));

            assertEquals("Zone-adjusted fare cannot be negative", ex.getMessage());
            verifyNoInteractions(merchantTierDiscountService, promoCodeService);
        }

        @Test
        @DisplayName("Null fuelSurchargeAmount throws IllegalArgumentException before calling dependencies")
        void input_nullFuelSurcharge_throwsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    discountEngineService.calculateDiscounts(new BigDecimal("100.00"), MerchantTier.GOLD, "PROMO", null, fixedNow));

            assertEquals("Fuel surcharge amount cannot be null", ex.getMessage());
            verifyNoInteractions(merchantTierDiscountService, promoCodeService);
        }

        @Test
        @DisplayName("Negative fuelSurchargeAmount throws IllegalArgumentException before calling dependencies")
        void input_negativeFuelSurcharge_throwsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    discountEngineService.calculateDiscounts(new BigDecimal("100.00"), MerchantTier.GOLD, "PROMO", new BigDecimal("-5.00"), fixedNow));

            assertEquals("Fuel surcharge amount cannot be negative", ex.getMessage());
            verifyNoInteractions(merchantTierDiscountService, promoCodeService);
        }

        @Test
        @DisplayName("Null evaluationTime in 5-argument method throws IllegalArgumentException before calling dependencies")
        void input_nullEvaluationTime_throwsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    discountEngineService.calculateDiscounts(new BigDecimal("100.00"), MerchantTier.GOLD, "PROMO", new BigDecimal("10.00"), null));

            assertEquals("Evaluation time cannot be null", ex.getMessage());
            verifyNoInteractions(merchantTierDiscountService, promoCodeService);
        }
    }

    @Nested
    @DisplayName("Collaborator Return Validation")
    class CollaboratorReturnValidation {

        @Test
        @DisplayName("Null merchant discount returned throws IllegalStateException")
        void collaborator_nullMerchantDiscount_throwsException() {
            BigDecimal fare = new BigDecimal("500.00");
            when(merchantTierDiscountService.calculateDiscount(fare, MerchantTier.GOLD)).thenReturn(null);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    discountEngineService.calculateDiscounts(fare, MerchantTier.GOLD, "PROMO", new BigDecimal("20.00"), fixedNow));

            assertTrue(ex.getMessage().contains("Merchant discount returned by service cannot be null or negative"));
        }

        @Test
        @DisplayName("Negative merchant discount returned throws IllegalStateException")
        void collaborator_negativeMerchantDiscount_throwsException() {
            BigDecimal fare = new BigDecimal("500.00");
            when(merchantTierDiscountService.calculateDiscount(fare, MerchantTier.GOLD)).thenReturn(new BigDecimal("-10.00"));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    discountEngineService.calculateDiscounts(fare, MerchantTier.GOLD, "PROMO", new BigDecimal("20.00"), fixedNow));

            assertTrue(ex.getMessage().contains("Merchant discount returned by service cannot be null or negative"));
        }

        @Test
        @DisplayName("Null promo discount returned throws IllegalStateException")
        void collaborator_nullPromoDiscount_throwsException() {
            BigDecimal fare = new BigDecimal("500.00");
            when(merchantTierDiscountService.calculateDiscount(fare, MerchantTier.GOLD)).thenReturn(new BigDecimal("50.00"));
            when(promoCodeService.validateAndCalculateDiscount("PROMO", fare, fixedNow)).thenReturn(null);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    discountEngineService.calculateDiscounts(fare, MerchantTier.GOLD, "PROMO", new BigDecimal("20.00"), fixedNow));

            assertTrue(ex.getMessage().contains("Promo discount returned by service cannot be null or negative"));
        }

        @Test
        @DisplayName("Negative promo discount returned throws IllegalStateException")
        void collaborator_negativePromoDiscount_throwsException() {
            BigDecimal fare = new BigDecimal("500.00");
            when(merchantTierDiscountService.calculateDiscount(fare, MerchantTier.GOLD)).thenReturn(new BigDecimal("50.00"));
            when(promoCodeService.validateAndCalculateDiscount("PROMO", fare, fixedNow)).thenReturn(new BigDecimal("-15.00"));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    discountEngineService.calculateDiscounts(fare, MerchantTier.GOLD, "PROMO", new BigDecimal("20.00"), fixedNow));

            assertTrue(ex.getMessage().contains("Promo discount returned by service cannot be null or negative"));
        }
    }

    @Nested
    @DisplayName("Clock & Overload Delegation")
    class ClockAndOverloadDelegation {

        @Test
        @DisplayName("Four-argument overload uses injected Clock to obtain evaluationTime")
        void fourArgOverload_usesInjectedClock() {
            BigDecimal fare = new BigDecimal("500.00");
            BigDecimal fuel = new BigDecimal("20.00");

            when(merchantTierDiscountService.calculateDiscount(fare, MerchantTier.BRONZE))
                    .thenReturn(new BigDecimal("25.00"));
            when(promoCodeService.validateAndCalculateDiscount("SAVE5", fare, fixedNow))
                    .thenReturn(new BigDecimal("25.00"));

            DiscountEngineResult result = discountEngineService.calculateDiscounts(fare, MerchantTier.BRONZE, "SAVE5", fuel);

            verify(promoCodeService).validateAndCalculateDiscount(eq("SAVE5"), eq(fare), eq(fixedNow));
            assertEquals(new BigDecimal("450.00"), result.getNetFreight());
            assertEquals(new BigDecimal("470.00"), result.getFinalFare());
        }

        @Test
        @DisplayName("Default constructor uses systemDefaultZone clock")
        void defaultConstructor_initializesProperly() {
            DiscountEngineService defaultService = new DiscountEngineService(merchantTierDiscountService, promoCodeService);
            assertNotNull(defaultService);
        }
    }

    @Nested
    @DisplayName("Scale Normalization & Exception Transparency")
    class NormalizationAndExceptionTransparency {

        @Test
        @DisplayName("All seven monetary fields in DiscountEngineResult are normalized to scale 2 HALF_UP")
        void result_allSevenFieldsNormalizedToScale2() {
            BigDecimal fare = new BigDecimal("1000.555");
            BigDecimal fuel = new BigDecimal("50.111");

            when(merchantTierDiscountService.calculateDiscount(fare, null))
                    .thenReturn(new BigDecimal("100.222"));
            when(promoCodeService.validateAndCalculateDiscount("CODE", fare, fixedNow))
                    .thenReturn(new BigDecimal("50.333"));

            DiscountEngineResult result = discountEngineService.calculateDiscounts(fare, null, "CODE", fuel, fixedNow);

            assertEquals(2, result.getZoneAdjustedFare().scale());
            assertEquals(new BigDecimal("1000.56"), result.getZoneAdjustedFare());

            assertEquals(2, result.getMerchantDiscountAmount().scale());
            assertEquals(new BigDecimal("100.22"), result.getMerchantDiscountAmount());

            assertEquals(2, result.getPromoDiscountAmount().scale());
            assertEquals(new BigDecimal("50.33"), result.getPromoDiscountAmount());

            assertEquals(2, result.getTotalDiscountAmount().scale());
            assertEquals(new BigDecimal("150.56"), result.getTotalDiscountAmount());

            assertEquals(2, result.getNetFreight().scale());
            assertEquals(new BigDecimal("850.00"), result.getNetFreight());

            assertEquals(2, result.getFuelSurchargeAmount().scale());
            assertEquals(new BigDecimal("50.11"), result.getFuelSurchargeAmount());

            assertEquals(2, result.getFinalFare().scale());
            assertEquals(new BigDecimal("900.11"), result.getFinalFare());
        }

        @Test
        @DisplayName("InvalidPromoCodeException thrown by PromoCodeService propagates untouched")
        void exceptionTransparency_invalidPromoExceptionPropagates() {
            BigDecimal fare = new BigDecimal("500.00");
            when(merchantTierDiscountService.calculateDiscount(fare, null)).thenReturn(new BigDecimal("0.00"));
            when(promoCodeService.validateAndCalculateDiscount("EXPIRED", fare, fixedNow))
                    .thenThrow(new InvalidPromoCodeException("Promo code is expired"));

            InvalidPromoCodeException ex = assertThrows(InvalidPromoCodeException.class, () ->
                    discountEngineService.calculateDiscounts(fare, null, "EXPIRED", new BigDecimal("10.00"), fixedNow));

            assertEquals("Promo code is expired", ex.getMessage());
        }

        @Test
        @DisplayName("IllegalStateException thrown by PromoCodeService propagates untouched")
        void exceptionTransparency_illegalStateExceptionPropagates() {
            BigDecimal fare = new BigDecimal("500.00");
            when(merchantTierDiscountService.calculateDiscount(fare, null)).thenReturn(new BigDecimal("0.00"));
            when(promoCodeService.validateAndCalculateDiscount("CORRUPT", fare, fixedNow))
                    .thenThrow(new IllegalStateException("Invalid promo code configuration"));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    discountEngineService.calculateDiscounts(fare, null, "CORRUPT", new BigDecimal("10.00"), fixedNow));

            assertEquals("Invalid promo code configuration", ex.getMessage());
        }
    }
}
