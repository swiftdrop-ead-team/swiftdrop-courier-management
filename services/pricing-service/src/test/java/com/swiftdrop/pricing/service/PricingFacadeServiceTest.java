package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.dto.PricingRequestDto;
import com.swiftdrop.pricing.dto.PricingResponseDto;
import com.swiftdrop.pricing.entity.DeliveryZone;
import com.swiftdrop.pricing.entity.MerchantTier;
import com.swiftdrop.pricing.entity.PriceQuote;
import com.swiftdrop.pricing.entity.PricingRule;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.exception.DeliveryRestrictionException;
import com.swiftdrop.pricing.exception.FuelRateNotFoundException;
import com.swiftdrop.pricing.exception.InactiveZoneException;
import com.swiftdrop.pricing.exception.InvalidPromoCodeException;
import com.swiftdrop.pricing.exception.UnsupportedPricingTierException;
import com.swiftdrop.pricing.exception.ZoneNotFoundException;
import com.swiftdrop.pricing.mapper.PriceQuoteMapper;
import com.swiftdrop.pricing.repository.PricingRuleRepository;
import com.swiftdrop.pricing.strategy.PricingContext;
import com.swiftdrop.pricing.strategy.PricingStrategy;
import com.swiftdrop.pricing.strategy.PricingStrategyRegistry;
import com.swiftdrop.pricing.strategy.StrategyPricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingFacadeServiceTest {

    @Mock
    private PricingStrategyRegistry strategyRegistry;

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private DeliveryZoneService deliveryZoneService;

    @Mock
    private FuelSurchargeService fuelSurchargeService;

    @Mock
    private DiscountEngineService discountEngineService;

    @Mock
    private PriceQuoteService priceQuoteService;

    @Mock
    private PriceQuoteMapper priceQuoteMapper;

    @Mock
    private PricingStrategy mockStrategy;

    private Clock fixedClock;
    private LocalDateTime fixedNow;
    private PricingFacadeService facadeService;

    @BeforeEach
    void setUp() {
        fixedNow = LocalDateTime.of(2026, 11, 13, 15, 0, 0);
        fixedClock = Clock.fixed(Instant.parse("2026-11-13T09:30:00Z"), ZoneId.of("UTC"));
        facadeService = new PricingFacadeService(
                strategyRegistry,
                pricingRuleRepository,
                deliveryZoneService,
                fuelSurchargeService,
                discountEngineService,
                priceQuoteService,
                priceQuoteMapper,
                fixedClock
        );
    }

    private PricingRequestDto createSampleRequest() {
        return PricingRequestDto.builder()
                .serviceType(ServiceType.STANDARD)
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("GREATER_COLOMBO")
                .physicalWeightKg(new BigDecimal("2.50"))
                .lengthCm(new BigDecimal("30.00"))
                .widthCm(new BigDecimal("20.00"))
                .heightCm(new BigDecimal("10.00"))
                .distanceKm(new BigDecimal("15.00"))
                .promoCode("WELCOME10")
                .orderReference("ORD-1001")
                .customerId("CUST-500")
                .build();
    }

    private PricingRule createSamplePricingRule() {
        return PricingRule.builder()
                .id(1L)
                .serviceType(ServiceType.STANDARD)
                .baseFare(new BigDecimal("350.00"))
                .baseWeightKg(new BigDecimal("1.00"))
                .perKgRate(new BigDecimal("125.00"))
                .perKmRate(new BigDecimal("15.00"))
                .volumetricDivisor(5000)
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .isActive(true)
                .build();
    }

    private StrategyPricingResult createSampleStrategyResult() {
        return StrategyPricingResult.builder()
                .serviceType(ServiceType.STANDARD)
                .baseFare(new BigDecimal("350.00"))
                .weightCharge(new BigDecimal("187.50"))
                .distanceCharge(new BigDecimal("225.00"))
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .volumetricWeightKg(new BigDecimal("1.20"))
                .billableWeightKg(new BigDecimal("2.50"))
                .extraWeightKg(new BigDecimal("1.50"))
                .subtotalFare(new BigDecimal("762.50"))
                .build();
    }

    private ZoneAndFuelPricingResult createSampleZoneFuelResult(DeliveryZone origin, DeliveryZone dest) {
        return ZoneAndFuelPricingResult.builder()
                .originZone(origin)
                .destinationZone(dest)
                .zoneMultiplier(new BigDecimal("1.1500"))
                .zoneAdjustedFare(new BigDecimal("876.88"))
                .fuelSurchargeRatePercentage(new BigDecimal("5.50"))
                .fuelSurchargeAmount(new BigDecimal("12.38"))
                .subtotalWithFuel(new BigDecimal("889.26"))
                .build();
    }

    private DiscountEngineResult createSampleDiscountResult() {
        return DiscountEngineResult.builder()
                .zoneAdjustedFare(new BigDecimal("876.88"))
                .merchantDiscountAmount(BigDecimal.ZERO)
                .promoDiscountAmount(new BigDecimal("87.69"))
                .totalDiscountAmount(new BigDecimal("87.69"))
                .netFreight(new BigDecimal("789.19"))
                .fuelSurchargeAmount(new BigDecimal("12.38"))
                .finalFare(new BigDecimal("801.57"))
                .build();
    }

    private PriceQuote createSampleQuote() {
        return PriceQuote.builder()
                .id(UUID.randomUUID())
                .serviceType(ServiceType.STANDARD)
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("GREATER_COLOMBO")
                .finalFare(new BigDecimal("801.57"))
                .currency("LKR")
                .expiresAt(fixedNow.plusMinutes(30))
                .build();
    }

    private PricingResponseDto createSampleResponseDto(UUID id) {
        return PricingResponseDto.builder()
                .id(id)
                .serviceType(ServiceType.STANDARD)
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("GREATER_COLOMBO")
                .finalFare(new BigDecimal("801.57"))
                .currency("LKR")
                .expiresAt(fixedNow.plusMinutes(30))
                .build();
    }

    @Test
    @DisplayName("Orchestrates full pricing calculation chain with null merchant tier")
    void testCalculateAndCreateQuote_success_nullMerchantTier() {
        PricingRequestDto request = createSampleRequest();
        PricingRule rule = createSamplePricingRule();
        StrategyPricingResult strategyResult = createSampleStrategyResult();

        DeliveryZone originZone = DeliveryZone.builder().zoneCode("COLOMBO_CENTRAL").zoneName("Colombo Central").baseZoneMultiplier(new BigDecimal("1.0000")).isActive(true).build();
        DeliveryZone destZone = DeliveryZone.builder().zoneCode("GREATER_COLOMBO").zoneName("Greater Colombo").baseZoneMultiplier(new BigDecimal("1.1500")).isActive(true).build();
        ZoneAndFuelPricingResult zoneFuelResult = createSampleZoneFuelResult(originZone, destZone);
        DiscountEngineResult discountResult = createSampleDiscountResult();
        PriceQuote quote = createSampleQuote();
        PricingResponseDto expectedDto = createSampleResponseDto(quote.getId());

        when(strategyRegistry.getStrategy(ServiceType.STANDARD)).thenReturn(mockStrategy);
        when(pricingRuleRepository.findByServiceTypeAndIsActiveTrue(ServiceType.STANDARD)).thenReturn(Optional.of(rule));
        when(mockStrategy.calculate(any(PricingContext.class), eq(rule))).thenReturn(strategyResult);
        when(deliveryZoneService.validateAndGetZone("COLOMBO_CENTRAL")).thenReturn(originZone);
        when(deliveryZoneService.validateAndGetZone("GREATER_COLOMBO")).thenReturn(destZone);
        when(fuelSurchargeService.calculateZoneAndFuelPricing(eq(strategyResult), eq(originZone), eq(destZone), any(LocalDateTime.class)))
                .thenReturn(zoneFuelResult);
        when(discountEngineService.calculateDiscounts(eq(zoneFuelResult.getZoneAdjustedFare()), isNull(), eq("WELCOME10"), eq(zoneFuelResult.getFuelSurchargeAmount()), any(LocalDateTime.class)))
                .thenReturn(discountResult);
        when(priceQuoteService.createAndSaveQuote(eq(request), eq(strategyResult), eq(zoneFuelResult.getZoneMultiplier()), eq(discountResult), any(LocalDateTime.class)))
                .thenReturn(quote);
        when(priceQuoteMapper.toDto(quote)).thenReturn(expectedDto);

        PricingResponseDto result = facadeService.calculateAndCreateQuote(request, null, fixedNow);

        assertNotNull(result);
        assertEquals(quote.getId(), result.getId());
        assertEquals("LKR", result.getCurrency());
        assertEquals(new BigDecimal("801.57"), result.getFinalFare());

        // Verify that discount engine received null merchantTier
        verify(discountEngineService).calculateDiscounts(
                eq(zoneFuelResult.getZoneAdjustedFare()),
                isNull(),
                eq("WELCOME10"),
                eq(zoneFuelResult.getFuelSurchargeAmount()),
                eq(fixedNow)
        );

        // Verify quote service received zone multiplier from zoneFuelResult
        verify(priceQuoteService).createAndSaveQuote(
                eq(request),
                eq(strategyResult),
                eq(new BigDecimal("1.1500")),
                eq(discountResult),
                eq(fixedNow)
        );
    }

    @Test
    @DisplayName("Passes non-null MerchantTier to discount engine when supplied")
    void testCalculateAndCreateQuote_withMerchantTier() {
        PricingRequestDto request = createSampleRequest();
        PricingRule rule = createSamplePricingRule();
        StrategyPricingResult strategyResult = createSampleStrategyResult();
        DeliveryZone originZone = DeliveryZone.builder().zoneCode("COLOMBO_CENTRAL").build();
        DeliveryZone destZone = DeliveryZone.builder().zoneCode("GREATER_COLOMBO").build();
        ZoneAndFuelPricingResult zoneFuelResult = createSampleZoneFuelResult(originZone, destZone);
        DiscountEngineResult discountResult = createSampleDiscountResult();
        PriceQuote quote = createSampleQuote();

        when(strategyRegistry.getStrategy(ServiceType.STANDARD)).thenReturn(mockStrategy);
        when(pricingRuleRepository.findByServiceTypeAndIsActiveTrue(ServiceType.STANDARD)).thenReturn(Optional.of(rule));
        when(mockStrategy.calculate(any(), any())).thenReturn(strategyResult);
        when(deliveryZoneService.validateAndGetZone("COLOMBO_CENTRAL")).thenReturn(originZone);
        when(deliveryZoneService.validateAndGetZone("GREATER_COLOMBO")).thenReturn(destZone);
        when(fuelSurchargeService.calculateZoneAndFuelPricing(any(), any(), any(), any())).thenReturn(zoneFuelResult);
        when(discountEngineService.calculateDiscounts(any(), eq(MerchantTier.GOLD), any(), any(), any())).thenReturn(discountResult);
        when(priceQuoteService.createAndSaveQuote(any(), any(), any(), any(), any())).thenReturn(quote);
        when(priceQuoteMapper.toDto(quote)).thenReturn(createSampleResponseDto(quote.getId()));

        facadeService.calculateAndCreateQuote(request, MerchantTier.GOLD, fixedNow);

        ArgumentCaptor<MerchantTier> tierCaptor = ArgumentCaptor.forClass(MerchantTier.class);
        verify(discountEngineService).calculateDiscounts(any(), tierCaptor.capture(), any(), any(), any());
        assertEquals(MerchantTier.GOLD, tierCaptor.getValue());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when request is null")
    void testCalculateAndCreateQuote_nullRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> facadeService.calculateAndCreateQuote(null, null));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when calculationTime is null")
    void testCalculateAndCreateQuote_nullCalculationTime() {
        PricingRequestDto request = createSampleRequest();
        assertThrows(IllegalArgumentException.class,
                () -> facadeService.calculateAndCreateQuote(request, null, null));
    }

    @Test
    @DisplayName("Propagates UnsupportedPricingTierException when registry throws")
    void testCalculateAndCreateQuote_unsupportedTier() {
        PricingRequestDto request = createSampleRequest();
        when(strategyRegistry.getStrategy(ServiceType.STANDARD))
                .thenThrow(new UnsupportedPricingTierException("No strategy for STANDARD"));

        assertThrows(UnsupportedPricingTierException.class,
                () -> facadeService.calculateAndCreateQuote(request, null));
    }

    @Test
    @DisplayName("Throws IllegalStateException when no active pricing rule is found")
    void testCalculateAndCreateQuote_missingPricingRule() {
        PricingRequestDto request = createSampleRequest();
        when(strategyRegistry.getStrategy(ServiceType.STANDARD)).thenReturn(mockStrategy);
        when(pricingRuleRepository.findByServiceTypeAndIsActiveTrue(ServiceType.STANDARD))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> facadeService.calculateAndCreateQuote(request, null));
        assertTrue(ex.getMessage().contains("No active pricing rule"));
    }

    @Test
    @DisplayName("Propagates DeliveryRestrictionException when strategy restriction is violated")
    void testCalculateAndCreateQuote_deliveryRestriction() {
        PricingRequestDto request = createSampleRequest();
        PricingRule rule = createSamplePricingRule();

        when(strategyRegistry.getStrategy(ServiceType.STANDARD)).thenReturn(mockStrategy);
        when(pricingRuleRepository.findByServiceTypeAndIsActiveTrue(ServiceType.STANDARD)).thenReturn(Optional.of(rule));
        when(mockStrategy.calculate(any(), any()))
                .thenThrow(new DeliveryRestrictionException("Package weight exceeds limit"));

        assertThrows(DeliveryRestrictionException.class,
                () -> facadeService.calculateAndCreateQuote(request, null));
    }

    @Test
    @DisplayName("Propagates ZoneNotFoundException when delivery zone does not exist")
    void testCalculateAndCreateQuote_zoneNotFound() {
        PricingRequestDto request = createSampleRequest();
        PricingRule rule = createSamplePricingRule();
        StrategyPricingResult strategyResult = createSampleStrategyResult();

        when(strategyRegistry.getStrategy(ServiceType.STANDARD)).thenReturn(mockStrategy);
        when(pricingRuleRepository.findByServiceTypeAndIsActiveTrue(ServiceType.STANDARD)).thenReturn(Optional.of(rule));
        when(mockStrategy.calculate(any(), any())).thenReturn(strategyResult);
        when(deliveryZoneService.validateAndGetZone("COLOMBO_CENTRAL"))
                .thenThrow(new ZoneNotFoundException("Zone not found: COLOMBO_CENTRAL"));

        assertThrows(ZoneNotFoundException.class,
                () -> facadeService.calculateAndCreateQuote(request, null));
    }

    @Test
    @DisplayName("Propagates InactiveZoneException when delivery zone is inactive")
    void testCalculateAndCreateQuote_inactiveZone() {
        PricingRequestDto request = createSampleRequest();
        PricingRule rule = createSamplePricingRule();
        StrategyPricingResult strategyResult = createSampleStrategyResult();

        when(strategyRegistry.getStrategy(ServiceType.STANDARD)).thenReturn(mockStrategy);
        when(pricingRuleRepository.findByServiceTypeAndIsActiveTrue(ServiceType.STANDARD)).thenReturn(Optional.of(rule));
        when(mockStrategy.calculate(any(), any())).thenReturn(strategyResult);
        when(deliveryZoneService.validateAndGetZone("COLOMBO_CENTRAL"))
                .thenThrow(new InactiveZoneException("Zone is inactive: COLOMBO_CENTRAL"));

        assertThrows(InactiveZoneException.class,
                () -> facadeService.calculateAndCreateQuote(request, null));
    }

    @Test
    @DisplayName("Propagates FuelRateNotFoundException when no active fuel rate index exists")
    void testCalculateAndCreateQuote_missingFuelRate() {
        PricingRequestDto request = createSampleRequest();
        PricingRule rule = createSamplePricingRule();
        StrategyPricingResult strategyResult = createSampleStrategyResult();
        DeliveryZone originZone = DeliveryZone.builder().zoneCode("COLOMBO_CENTRAL").build();
        DeliveryZone destZone = DeliveryZone.builder().zoneCode("GREATER_COLOMBO").build();

        when(strategyRegistry.getStrategy(ServiceType.STANDARD)).thenReturn(mockStrategy);
        when(pricingRuleRepository.findByServiceTypeAndIsActiveTrue(ServiceType.STANDARD)).thenReturn(Optional.of(rule));
        when(mockStrategy.calculate(any(), any())).thenReturn(strategyResult);
        when(deliveryZoneService.validateAndGetZone("COLOMBO_CENTRAL")).thenReturn(originZone);
        when(deliveryZoneService.validateAndGetZone("GREATER_COLOMBO")).thenReturn(destZone);
        when(fuelSurchargeService.calculateZoneAndFuelPricing(any(), any(), any(), any()))
                .thenThrow(new FuelRateNotFoundException("No active fuel rate found"));

        assertThrows(FuelRateNotFoundException.class,
                () -> facadeService.calculateAndCreateQuote(request, null));
    }

    @Test
    @DisplayName("Propagates InvalidPromoCodeException when promo code is invalid")
    void testCalculateAndCreateQuote_invalidPromoCode() {
        PricingRequestDto request = createSampleRequest();
        PricingRule rule = createSamplePricingRule();
        StrategyPricingResult strategyResult = createSampleStrategyResult();
        DeliveryZone originZone = DeliveryZone.builder().zoneCode("COLOMBO_CENTRAL").build();
        DeliveryZone destZone = DeliveryZone.builder().zoneCode("GREATER_COLOMBO").build();
        ZoneAndFuelPricingResult zoneFuelResult = createSampleZoneFuelResult(originZone, destZone);

        when(strategyRegistry.getStrategy(ServiceType.STANDARD)).thenReturn(mockStrategy);
        when(pricingRuleRepository.findByServiceTypeAndIsActiveTrue(ServiceType.STANDARD)).thenReturn(Optional.of(rule));
        when(mockStrategy.calculate(any(), any())).thenReturn(strategyResult);
        when(deliveryZoneService.validateAndGetZone("COLOMBO_CENTRAL")).thenReturn(originZone);
        when(deliveryZoneService.validateAndGetZone("GREATER_COLOMBO")).thenReturn(destZone);
        when(fuelSurchargeService.calculateZoneAndFuelPricing(any(), any(), any(), any())).thenReturn(zoneFuelResult);
        when(discountEngineService.calculateDiscounts(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidPromoCodeException("Promo code expired"));

        assertThrows(InvalidPromoCodeException.class,
                () -> facadeService.calculateAndCreateQuote(request, null));
    }

    @Test
    @DisplayName("getQuote delegates to priceQuoteService.getQuoteDto")
    void testGetQuote_delegates() {
        UUID quoteId = UUID.randomUUID();
        PricingResponseDto expected = createSampleResponseDto(quoteId);
        when(priceQuoteService.getQuoteDto(quoteId)).thenReturn(expected);

        PricingResponseDto result = facadeService.getQuote(quoteId);

        assertNotNull(result);
        assertEquals(quoteId, result.getId());
        verify(priceQuoteService).getQuoteDto(quoteId);
    }

    @Test
    @DisplayName("getActiveQuote delegates to priceQuoteService.getActiveQuoteDto")
    void testGetActiveQuote_delegates() {
        UUID quoteId = UUID.randomUUID();
        PricingResponseDto expected = createSampleResponseDto(quoteId);
        when(priceQuoteService.getActiveQuoteDto(quoteId)).thenReturn(expected);

        PricingResponseDto result = facadeService.getActiveQuote(quoteId);

        assertNotNull(result);
        assertEquals(quoteId, result.getId());
        verify(priceQuoteService).getActiveQuoteDto(quoteId);
    }

    @Test
    @DisplayName("getQuote throws IllegalArgumentException when ID is null")
    void testGetQuote_nullIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> facadeService.getQuote(null));
        verifyNoInteractions(priceQuoteService);
    }

    @Test
    @DisplayName("getActiveQuote throws IllegalArgumentException when ID is null")
    void testGetActiveQuote_nullIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> facadeService.getActiveQuote(null));
        verifyNoInteractions(priceQuoteService);
    }
}
