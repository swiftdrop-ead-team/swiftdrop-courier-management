package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.entity.MerchantTier;
import com.swiftdrop.pricing.entity.MerchantTierDiscount;
import com.swiftdrop.pricing.repository.MerchantTierDiscountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantTierDiscountServiceTest {

    @Mock
    private MerchantTierDiscountRepository merchantTierDiscountRepository;

    @InjectMocks
    private MerchantTierDiscountService merchantTierDiscountService;

    @Nested
    @DisplayName("Valid Tier Discount Calculations")
    class ValidTierCalculations {

        @Test
        @DisplayName("Bronze tier applies configured percentage discount (e.g. 5.00%)")
        void calculateDiscount_bronzeTier_returnsCorrectDiscount() {
            MerchantTierDiscount bronzeDiscount = MerchantTierDiscount.builder()
                    .tier(MerchantTier.BRONZE)
                    .discountPercentage(new BigDecimal("5.00"))
                    .minMonthlyShipments(0)
                    .isActive(true)
                    .build();

            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.BRONZE))
                    .thenReturn(Optional.of(bronzeDiscount));

            BigDecimal fare = new BigDecimal("1000.00");
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, MerchantTier.BRONZE);

            assertEquals(new BigDecimal("50.00"), discount);
            assertEquals(2, discount.scale());
            verify(merchantTierDiscountRepository).findByTierAndIsActiveTrue(MerchantTier.BRONZE);
        }

        @Test
        @DisplayName("Silver tier applies configured percentage discount (e.g. 10.00%)")
        void calculateDiscount_silverTier_returnsCorrectDiscount() {
            MerchantTierDiscount silverDiscount = MerchantTierDiscount.builder()
                    .tier(MerchantTier.SILVER)
                    .discountPercentage(new BigDecimal("10.00"))
                    .minMonthlyShipments(50)
                    .isActive(true)
                    .build();

            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.SILVER))
                    .thenReturn(Optional.of(silverDiscount));

            BigDecimal fare = new BigDecimal("850.00");
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, MerchantTier.SILVER);

            assertEquals(new BigDecimal("85.00"), discount);
            assertEquals(2, discount.scale());
        }

        @Test
        @DisplayName("Gold tier applies configured percentage discount (e.g. 15.50%)")
        void calculateDiscount_goldTier_returnsCorrectDiscount() {
            MerchantTierDiscount goldDiscount = MerchantTierDiscount.builder()
                    .tier(MerchantTier.GOLD)
                    .discountPercentage(new BigDecimal("15.50"))
                    .minMonthlyShipments(250)
                    .isActive(true)
                    .build();

            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.GOLD))
                    .thenReturn(Optional.of(goldDiscount));

            BigDecimal fare = new BigDecimal("1200.00");
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, MerchantTier.GOLD);

            assertEquals(new BigDecimal("186.00"), discount);
            assertEquals(2, discount.scale());
        }

        @Test
        @DisplayName("Platinum tier applies configured percentage discount (e.g. 20.00%)")
        void calculateDiscount_platinumTier_returnsCorrectDiscount() {
            MerchantTierDiscount platinumDiscount = MerchantTierDiscount.builder()
                    .tier(MerchantTier.PLATINUM)
                    .discountPercentage(new BigDecimal("20.00"))
                    .minMonthlyShipments(500)
                    .isActive(true)
                    .build();

            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.PLATINUM))
                    .thenReturn(Optional.of(platinumDiscount));

            BigDecimal fare = new BigDecimal("2500.00");
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, MerchantTier.PLATINUM);

            assertEquals(new BigDecimal("500.00"), discount);
            assertEquals(2, discount.scale());
        }

        @Test
        @DisplayName("High minMonthlyShipments is ignored in Step 5 and does not block discount")
        void calculateDiscount_minMonthlyShipmentsIgnored() {
            MerchantTierDiscount discountConfig = MerchantTierDiscount.builder()
                    .tier(MerchantTier.PLATINUM)
                    .discountPercentage(new BigDecimal("25.00"))
                    .minMonthlyShipments(10000)
                    .isActive(true)
                    .build();

            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.PLATINUM))
                    .thenReturn(Optional.of(discountConfig));

            BigDecimal fare = new BigDecimal("1000.00");
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, MerchantTier.PLATINUM);

            assertEquals(new BigDecimal("250.00"), discount);
        }
    }

    @Nested
    @DisplayName("Null, Inactive, and Unconfigured Tiers")
    class NullAndInactiveTiers {

        @Test
        @DisplayName("Null merchant tier returns 0.00 and avoids repository query")
        void calculateDiscount_nullMerchantTier_returnsZero() {
            BigDecimal fare = new BigDecimal("1000.00");
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, null);

            assertEquals(new BigDecimal("0.00"), discount);
            assertEquals(2, discount.scale());
            verifyNoInteractions(merchantTierDiscountRepository);
        }

        @Test
        @DisplayName("Inactive merchant tier returns 0.00 when repository returns empty for active query")
        void calculateDiscount_inactiveTier_returnsZero() {
            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.GOLD))
                    .thenReturn(Optional.empty());

            BigDecimal fare = new BigDecimal("1000.00");
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, MerchantTier.GOLD);

            assertEquals(new BigDecimal("0.00"), discount);
            assertEquals(2, discount.scale());
        }

        @Test
        @DisplayName("Unconfigured merchant tier returns 0.00 when record does not exist")
        void calculateDiscount_unconfiguredTier_returnsZero() {
            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.BRONZE))
                    .thenReturn(Optional.empty());

            BigDecimal fare = new BigDecimal("500.00");
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, MerchantTier.BRONZE);

            assertEquals(new BigDecimal("0.00"), discount);
            assertEquals(2, discount.scale());
        }
    }

    @Nested
    @DisplayName("Input Validation (Null and Negative Fares)")
    class InputValidation {

        @Test
        @DisplayName("Null zoneAdjustedFare throws IllegalArgumentException")
        void calculateDiscount_nullFare_throwsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    merchantTierDiscountService.calculateDiscount(null, MerchantTier.GOLD));

            assertEquals("Zone-adjusted fare cannot be null", ex.getMessage());
            verifyNoInteractions(merchantTierDiscountRepository);
        }

        @Test
        @DisplayName("Negative zoneAdjustedFare throws IllegalArgumentException")
        void calculateDiscount_negativeFare_throwsIllegalArgumentException() {
            BigDecimal negativeFare = new BigDecimal("-0.01");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    merchantTierDiscountService.calculateDiscount(negativeFare, MerchantTier.GOLD));

            assertEquals("Zone-adjusted fare cannot be negative", ex.getMessage());
            verifyNoInteractions(merchantTierDiscountRepository);
        }

        @Test
        @DisplayName("Zero zoneAdjustedFare returns 0.00 without error")
        void calculateDiscount_zeroFare_returnsZero() {
            MerchantTierDiscount discountConfig = MerchantTierDiscount.builder()
                    .tier(MerchantTier.GOLD)
                    .discountPercentage(new BigDecimal("15.00"))
                    .isActive(true)
                    .build();

            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.GOLD))
                    .thenReturn(Optional.of(discountConfig));

            BigDecimal discount = merchantTierDiscountService.calculateDiscount(BigDecimal.ZERO, MerchantTier.GOLD);

            assertEquals(new BigDecimal("0.00"), discount);
            assertEquals(2, discount.scale());
        }
    }

    @Nested
    @DisplayName("Negative or Zero Configured Rates Defense")
    class ConfiguredRatesDefense {

        @Test
        @DisplayName("Negative configured discount percentage safely returns 0.00")
        void calculateDiscount_negativePercentage_returnsZero() {
            MerchantTierDiscount negativeConfig = MerchantTierDiscount.builder()
                    .tier(MerchantTier.SILVER)
                    .discountPercentage(new BigDecimal("-10.00"))
                    .isActive(true)
                    .build();

            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.SILVER))
                    .thenReturn(Optional.of(negativeConfig));

            BigDecimal fare = new BigDecimal("1000.00");
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, MerchantTier.SILVER);

            assertEquals(new BigDecimal("0.00"), discount);
            assertEquals(2, discount.scale());
        }

        @Test
        @DisplayName("Zero configured discount percentage returns 0.00")
        void calculateDiscount_zeroPercentage_returnsZero() {
            MerchantTierDiscount zeroConfig = MerchantTierDiscount.builder()
                    .tier(MerchantTier.BRONZE)
                    .discountPercentage(BigDecimal.ZERO)
                    .isActive(true)
                    .build();

            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.BRONZE))
                    .thenReturn(Optional.of(zeroConfig));

            BigDecimal fare = new BigDecimal("1000.00");
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, MerchantTier.BRONZE);

            assertEquals(new BigDecimal("0.00"), discount);
            assertEquals(2, discount.scale());
        }

        @Test
        @DisplayName("Null configured discount percentage safely returns 0.00")
        void calculateDiscount_nullPercentage_returnsZero() {
            MerchantTierDiscount nullConfig = MerchantTierDiscount.builder()
                    .tier(MerchantTier.GOLD)
                    .discountPercentage(null)
                    .isActive(true)
                    .build();

            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.GOLD))
                    .thenReturn(Optional.of(nullConfig));

            BigDecimal fare = new BigDecimal("1000.00");
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, MerchantTier.GOLD);

            assertEquals(new BigDecimal("0.00"), discount);
            assertEquals(2, discount.scale());
        }
    }

    @Nested
    @DisplayName("Monetary Precision & HALF_UP Rounding Boundaries")
    class RoundingBoundaries {

        @ParameterizedTest(name = "Fare {0} with {1}% discount rounds to {2}")
        @CsvSource({
                "125.55, 7.55, 9.48",   // 125.55 * 0.0755 = 9.479025 -> 9.48
                "100.05, 5.50, 5.50",   // 100.05 * 0.0550 = 5.502750 -> 5.50
                "10.00,  1.25, 0.13",   // 10.00 * 0.0125 = 0.125000 -> 0.13 (HALF_UP boundary: 5 rounds up)
                "10.00,  1.24, 0.12",   // 10.00 * 0.0124 = 0.124000 -> 0.12 (rounds down)
                "33.33,  3.33, 1.11"    // 33.33 * 0.0333 = 1.109889 -> 1.11
        })
        void calculateDiscount_roundingBoundaries_halfUp(String fareStr, String rateStr, String expectedStr) {
            MerchantTierDiscount config = MerchantTierDiscount.builder()
                    .tier(MerchantTier.GOLD)
                    .discountPercentage(new BigDecimal(rateStr))
                    .isActive(true)
                    .build();

            when(merchantTierDiscountRepository.findByTierAndIsActiveTrue(MerchantTier.GOLD))
                    .thenReturn(Optional.of(config));

            BigDecimal fare = new BigDecimal(fareStr);
            BigDecimal discount = merchantTierDiscountService.calculateDiscount(fare, MerchantTier.GOLD);

            assertEquals(new BigDecimal(expectedStr), discount);
            assertEquals(2, discount.scale());
        }
    }
}
