package com.swiftdrop.pricing.service;

import com.swiftdrop.pricing.dto.PricingRequestDto;
import com.swiftdrop.pricing.dto.PricingResponseDto;
import com.swiftdrop.pricing.entity.PriceQuote;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.exception.QuoteExpiredException;
import com.swiftdrop.pricing.exception.QuoteNotFoundException;
import com.swiftdrop.pricing.mapper.PriceQuoteMapper;
import com.swiftdrop.pricing.repository.PriceQuoteRepository;
import com.swiftdrop.pricing.strategy.StrategyPricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceQuoteServiceTest {

    @Mock
    private PriceQuoteRepository priceQuoteRepository;

    private PriceQuoteMapper priceQuoteMapper;
    private Clock fixedClock;
    private LocalDateTime fixedNow;
    private PriceQuoteService service;

    private static final long DEFAULT_TTL_MINUTES = 30L;

    @BeforeEach
    void setUp() {
        priceQuoteMapper = new PriceQuoteMapper();
        ZoneId zoneId = ZoneId.of("Asia/Colombo");
        Instant fixedInstant = Instant.parse("2026-11-10T10:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, zoneId);
        fixedNow = LocalDateTime.now(fixedClock);

        service = new PriceQuoteService(
                priceQuoteRepository,
                priceQuoteMapper,
                DEFAULT_TTL_MINUTES,
                fixedClock
        );
    }

    private PricingRequestDto buildValidRequest() {
        return PricingRequestDto.builder()
                .serviceType(ServiceType.STANDARD)
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("GREATER_COLOMBO")
                .physicalWeightKg(new BigDecimal("2.50"))
                .distanceKm(new BigDecimal("15.00"))
                .lengthCm(new BigDecimal("20.00"))
                .widthCm(new BigDecimal("15.00"))
                .heightCm(new BigDecimal("10.00"))
                .promoCode("SAVE10")
                .customerId("CUST-001")
                .build();
    }

    private StrategyPricingResult buildValidStrategyResult() {
        return StrategyPricingResult.builder()
                .serviceType(ServiceType.STANDARD)
                .baseFare(new BigDecimal("350.00"))
                .weightCharge(new BigDecimal("187.50"))
                .distanceCharge(new BigDecimal("200.00"))
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .volumetricWeightKg(new BigDecimal("0.80"))
                .billableWeightKg(new BigDecimal("2.50"))
                .extraWeightKg(new BigDecimal("1.50"))
                .subtotalFare(new BigDecimal("737.50"))
                .build();
    }

    private DiscountEngineResult buildValidDiscountResult() {
        return DiscountEngineResult.builder()
                .zoneAdjustedFare(new BigDecimal("848.13"))
                .merchantDiscountAmount(new BigDecimal("42.41"))
                .promoDiscountAmount(new BigDecimal("84.81"))
                .totalDiscountAmount(new BigDecimal("127.22"))
                .netFreight(new BigDecimal("720.91"))
                .fuelSurchargeAmount(new BigDecimal("11.00"))
                .finalFare(new BigDecimal("731.91"))
                .build();
    }

    private PriceQuote.PriceQuoteBuilder validQuoteBuilder() {
        return PriceQuote.builder()
                .id(UUID.randomUUID())
                .serviceType(ServiceType.STANDARD)
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("GREATER_COLOMBO")
                .physicalWeightKg(new BigDecimal("2.50"))
                .volumetricWeightKg(new BigDecimal("0.80"))
                .billableWeightKg(new BigDecimal("2.50"))
                .distanceKm(new BigDecimal("15.00"))
                .baseFare(new BigDecimal("350.00"))
                .weightCharge(new BigDecimal("187.50"))
                .distanceCharge(new BigDecimal("200.00"))
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .zoneMultiplier(new BigDecimal("1.1500"))
                .fuelSurchargeAmount(new BigDecimal("11.00"))
                .merchantDiscountAmount(new BigDecimal("42.41"))
                .promoDiscountAmount(new BigDecimal("84.81"))
                .finalFare(new BigDecimal("731.91"))
                .currency("LKR")
                .expiresAt(fixedNow.plusMinutes(DEFAULT_TTL_MINUTES));
    }

    @Nested
    @DisplayName("TTL Configuration Validation")
    class TtlConfigurationValidation {

        @Test
        @DisplayName("Positive TTL is accepted during construction")
        void positiveTtl_constructsSuccessfully() {
            assertDoesNotThrow(() -> new PriceQuoteService(priceQuoteRepository, priceQuoteMapper, 15L, fixedClock));
            assertDoesNotThrow(() -> new PriceQuoteService(priceQuoteRepository, priceQuoteMapper, 60L, null));
        }

        @Test
        @DisplayName("Zero TTL is rejected during construction with IllegalStateException")
        void zeroTtl_throwsIllegalStateException() {
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    new PriceQuoteService(priceQuoteRepository, priceQuoteMapper, 0L, fixedClock));
            assertTrue(ex.getMessage().contains("must be strictly greater than zero"));
        }

        @Test
        @DisplayName("Negative TTL is rejected during construction with IllegalStateException")
        void negativeTtl_throwsIllegalStateException() {
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    new PriceQuoteService(priceQuoteRepository, priceQuoteMapper, -5L, fixedClock));
            assertTrue(ex.getMessage().contains("must be strictly greater than zero"));
        }
    }

    @Nested
    @DisplayName("Quote Creation")
    class QuoteCreation {

        @Test
        @DisplayName("createAndSaveQuote (5-arg) derives expiresAt strictly from supplied creationTime")
        void explicitCreationTime_derivesExpiresAtFromSuppliedTime() {
            PricingRequestDto request = buildValidRequest();
            StrategyPricingResult strategyResult = buildValidStrategyResult();
            DiscountEngineResult discountResult = buildValidDiscountResult();
            BigDecimal zoneMultiplier = new BigDecimal("1.1500");
            LocalDateTime customCreationTime = fixedNow.plusHours(2);

            when(priceQuoteRepository.save(any(PriceQuote.class))).thenAnswer(inv -> inv.getArgument(0));

            PriceQuote saved = service.createAndSaveQuote(
                    request, strategyResult, zoneMultiplier, discountResult, customCreationTime);

            assertNotNull(saved);
            assertEquals(ServiceType.STANDARD, saved.getServiceType());
            assertEquals("COLOMBO_CENTRAL", saved.getOriginZone());
            assertEquals("GREATER_COLOMBO", saved.getDestinationZone());
            assertEquals(0, new BigDecimal("2.50").compareTo(saved.getPhysicalWeightKg()));
            assertEquals(0, new BigDecimal("0.80").compareTo(saved.getVolumetricWeightKg()));
            assertEquals(0, new BigDecimal("2.50").compareTo(saved.getBillableWeightKg()));
            assertEquals(0, new BigDecimal("15.00").compareTo(saved.getDistanceKm()));
            assertEquals(0, new BigDecimal("350.00").compareTo(saved.getBaseFare()));
            assertEquals(0, new BigDecimal("187.50").compareTo(saved.getWeightCharge()));
            assertEquals(0, new BigDecimal("200.00").compareTo(saved.getDistanceCharge()));
            assertEquals(0, new BigDecimal("1.0000").compareTo(saved.getUrgencyMultiplier()));
            assertEquals(0, new BigDecimal("1.1500").compareTo(saved.getZoneMultiplier()));
            assertEquals(0, new BigDecimal("11.00").compareTo(saved.getFuelSurchargeAmount()));
            assertEquals(0, new BigDecimal("42.41").compareTo(saved.getMerchantDiscountAmount()));
            assertEquals(0, new BigDecimal("84.81").compareTo(saved.getPromoDiscountAmount()));
            assertEquals(0, new BigDecimal("731.91").compareTo(saved.getFinalFare()));
            assertEquals("LKR", saved.getCurrency());

            // Timestamp consistency: derives from customCreationTime
            assertEquals(customCreationTime.plusMinutes(DEFAULT_TTL_MINUTES), saved.getExpiresAt());

            ArgumentCaptor<PriceQuote> captor = ArgumentCaptor.forClass(PriceQuote.class);
            verify(priceQuoteRepository, times(1)).save(captor.capture());
            assertEquals(customCreationTime.plusMinutes(DEFAULT_TTL_MINUTES), captor.getValue().getExpiresAt());
        }

        @Test
        @DisplayName("createAndSaveQuote (4-arg) uses clock reading for creationTime")
        void fourArgOverload_usesClockForCreationTime() {
            PricingRequestDto request = buildValidRequest();
            StrategyPricingResult strategyResult = buildValidStrategyResult();
            DiscountEngineResult discountResult = buildValidDiscountResult();
            BigDecimal zoneMultiplier = new BigDecimal("1.1500");

            when(priceQuoteRepository.save(any(PriceQuote.class))).thenAnswer(inv -> inv.getArgument(0));

            PriceQuote saved = service.createAndSaveQuote(request, strategyResult, zoneMultiplier, discountResult);

            assertNotNull(saved);
            assertEquals(fixedNow.plusMinutes(DEFAULT_TTL_MINUTES), saved.getExpiresAt());
            verify(priceQuoteRepository, times(1)).save(any(PriceQuote.class));
        }

        @Test
        @DisplayName("createAndSaveQuote rejects null request")
        void nullRequest_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createAndSaveQuote(null, buildValidStrategyResult(), BigDecimal.ONE, buildValidDiscountResult(), fixedNow));
        }

        @Test
        @DisplayName("createAndSaveQuote rejects null strategyResult")
        void nullStrategyResult_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createAndSaveQuote(buildValidRequest(), null, BigDecimal.ONE, buildValidDiscountResult(), fixedNow));
        }

        @Test
        @DisplayName("createAndSaveQuote rejects null discountResult")
        void nullDiscountResult_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createAndSaveQuote(buildValidRequest(), buildValidStrategyResult(), BigDecimal.ONE, null, fixedNow));
        }

        @Test
        @DisplayName("createAndSaveQuote rejects null zoneMultiplier")
        void nullZoneMultiplier_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createAndSaveQuote(buildValidRequest(), buildValidStrategyResult(), null, buildValidDiscountResult(), fixedNow));
        }

        @Test
        @DisplayName("createAndSaveQuote rejects non-positive zoneMultiplier")
        void nonPositiveZoneMultiplier_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createAndSaveQuote(buildValidRequest(), buildValidStrategyResult(), BigDecimal.ZERO, buildValidDiscountResult(), fixedNow));
            assertThrows(IllegalArgumentException.class, () ->
                    service.createAndSaveQuote(buildValidRequest(), buildValidStrategyResult(), new BigDecimal("-1.0"), buildValidDiscountResult(), fixedNow));
        }

        @Test
        @DisplayName("createAndSaveQuote rejects null creationTime")
        void nullCreationTime_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createAndSaveQuote(buildValidRequest(), buildValidStrategyResult(), BigDecimal.ONE, buildValidDiscountResult(), null));
        }
    }

    @Nested
    @DisplayName("saveQuote Validation")
    class SaveQuoteValidation {

        @Test
        @DisplayName("saveQuote rejects null quote")
        void nullQuote_throwsException() {
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(null));
        }

        @Test
        @DisplayName("saveQuote rejects null serviceType")
        void nullServiceType_throwsException() {
            PriceQuote quote = validQuoteBuilder().serviceType(null).build();
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(quote));
        }

        @Test
        @DisplayName("saveQuote rejects blank originZone and destinationZone")
        void blankZones_throwException() {
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().originZone("").build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().originZone("   ").build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().originZone(null).build()));

            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().destinationZone("").build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().destinationZone("   ").build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().destinationZone(null).build()));
        }

        @Test
        @DisplayName("saveQuote validates weight constraints (physical > 0, volumetric >= 0, billable > 0)")
        void weightConstraints_validated() {
            // Physical <= 0
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().physicalWeightKg(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().physicalWeightKg(BigDecimal.ZERO).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().physicalWeightKg(new BigDecimal("-0.5")).build()));

            // Volumetric < 0
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().volumetricWeightKg(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().volumetricWeightKg(new BigDecimal("-0.1")).build()));

            // Billable <= 0
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().billableWeightKg(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().billableWeightKg(BigDecimal.ZERO).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().billableWeightKg(new BigDecimal("-1.0")).build()));
        }

        @Test
        @DisplayName("saveQuote validates distanceKm >= 0")
        void distanceKm_validated() {
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().distanceKm(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().distanceKm(new BigDecimal("-1.0")).build()));
        }

        @Test
        @DisplayName("saveQuote validates fare breakdown >= 0")
        void fareBreakdown_validated() {
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().baseFare(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().baseFare(new BigDecimal("-10.00")).build()));

            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().weightCharge(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().weightCharge(new BigDecimal("-10.00")).build()));

            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().distanceCharge(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().distanceCharge(new BigDecimal("-10.00")).build()));
        }

        @Test
        @DisplayName("saveQuote validates multipliers (urgency >= 1.0000, zone > 0)")
        void multipliers_validated() {
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().urgencyMultiplier(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().urgencyMultiplier(new BigDecimal("0.9999")).build()));

            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().zoneMultiplier(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().zoneMultiplier(BigDecimal.ZERO).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().zoneMultiplier(new BigDecimal("-0.5")).build()));
        }

        @Test
        @DisplayName("saveQuote validates discount and surcharge amounts >= 0")
        void discountAndSurcharges_validated() {
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().fuelSurchargeAmount(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().fuelSurchargeAmount(new BigDecimal("-1.00")).build()));

            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().merchantDiscountAmount(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().merchantDiscountAmount(new BigDecimal("-1.00")).build()));

            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().promoDiscountAmount(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().promoDiscountAmount(new BigDecimal("-1.00")).build()));
        }

        @Test
        @DisplayName("saveQuote validates finalFare >= 0")
        void finalFare_validated() {
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().finalFare(null).build()));
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(validQuoteBuilder().finalFare(new BigDecimal("-1.00")).build()));
        }

        @Test
        @DisplayName("saveQuote rejects null expiresAt")
        void nullExpiresAt_throwsException() {
            PriceQuote quote = validQuoteBuilder().expiresAt(null).build();
            assertThrows(IllegalArgumentException.class, () -> service.saveQuote(quote));
        }

        @Test
        @DisplayName("saveQuote rejects already-expired quote (now > expiresAt)")
        void alreadyExpiredQuote_throwsException() {
            PriceQuote quote = validQuoteBuilder().expiresAt(fixedNow.minusSeconds(1)).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.saveQuote(quote));
            assertTrue(ex.getMessage().contains("Cannot persist an already-expired quote"));
        }

        @Test
        @DisplayName("saveQuote accepts quote at exact expiration boundary (now == expiresAt)")
        void exactBoundary_saveAccepted() {
            PriceQuote quote = validQuoteBuilder().expiresAt(fixedNow).build();
            when(priceQuoteRepository.save(quote)).thenReturn(quote);

            PriceQuote saved = service.saveQuote(quote);
            assertNotNull(saved);
            verify(priceQuoteRepository, times(1)).save(quote);
        }

        @Test
        @DisplayName("saveQuote accepts active quote (now < expiresAt)")
        void activeQuote_saveAccepted() {
            PriceQuote quote = validQuoteBuilder().expiresAt(fixedNow.plusSeconds(1)).build();
            when(priceQuoteRepository.save(quote)).thenReturn(quote);

            PriceQuote saved = service.saveQuote(quote);
            assertNotNull(saved);
            verify(priceQuoteRepository, times(1)).save(quote);
        }
    }

    @Nested
    @DisplayName("Historical Retrieval")
    class HistoricalRetrieval {

        @Test
        @DisplayName("getQuote returns quote when found, even if expired")
        void returnsQuote_evenIfExpired() {
            UUID id = UUID.randomUUID();
            PriceQuote expiredQuote = validQuoteBuilder()
                    .id(id)
                    .expiresAt(fixedNow.minusHours(1))
                    .build();

            when(priceQuoteRepository.findById(id)).thenReturn(Optional.of(expiredQuote));

            PriceQuote result = service.getQuote(id);
            assertNotNull(result);
            assertEquals(id, result.getId());
            assertEquals(expiredQuote.getExpiresAt(), result.getExpiresAt());
        }

        @Test
        @DisplayName("getQuote throws QuoteNotFoundException when id is not found")
        void missingId_throwsQuoteNotFoundException() {
            UUID id = UUID.randomUUID();
            when(priceQuoteRepository.findById(id)).thenReturn(Optional.empty());

            QuoteNotFoundException ex = assertThrows(QuoteNotFoundException.class, () -> service.getQuote(id));
            assertTrue(ex.getMessage().contains(id.toString()));
        }

        @Test
        @DisplayName("getQuoteDto maps found quote to DTO")
        void getQuoteDto_mapsToDto() {
            UUID id = UUID.randomUUID();
            PriceQuote quote = validQuoteBuilder().id(id).build();
            when(priceQuoteRepository.findById(id)).thenReturn(Optional.of(quote));

            PricingResponseDto dto = service.getQuoteDto(id);
            assertNotNull(dto);
            assertEquals(id, dto.getId());
            assertEquals(quote.getFinalFare().setScale(2), dto.getFinalFare());
            assertNull(dto.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Active Retrieval")
    class ActiveRetrieval {

        @Test
        @DisplayName("getActiveQuote returns quote when evaluationTime < expiresAt")
        void evaluationTimeBeforeExpiresAt_returnsQuote() {
            UUID id = UUID.randomUUID();
            PriceQuote quote = validQuoteBuilder()
                    .id(id)
                    .expiresAt(fixedNow.plusMinutes(10))
                    .build();

            when(priceQuoteRepository.findById(id)).thenReturn(Optional.of(quote));

            PriceQuote result = service.getActiveQuote(id, fixedNow);
            assertNotNull(result);
            assertEquals(id, result.getId());
        }

        @Test
        @DisplayName("getActiveQuote returns quote at exact expiration boundary (evaluationTime == expiresAt)")
        void evaluationTimeEqualsExpiresAt_boundaryIsValid() {
            UUID id = UUID.randomUUID();
            PriceQuote quote = validQuoteBuilder()
                    .id(id)
                    .expiresAt(fixedNow)
                    .build();

            when(priceQuoteRepository.findById(id)).thenReturn(Optional.of(quote));

            PriceQuote result = service.getActiveQuote(id, fixedNow);
            assertNotNull(result);
            assertEquals(id, result.getId());
        }

        @Test
        @DisplayName("getActiveQuote throws QuoteExpiredException when evaluationTime > expiresAt")
        void evaluationTimeAfterExpiresAt_throwsQuoteExpiredException() {
            UUID id = UUID.randomUUID();
            LocalDateTime expiry = fixedNow.minusSeconds(1);
            PriceQuote quote = validQuoteBuilder()
                    .id(id)
                    .expiresAt(expiry)
                    .build();

            when(priceQuoteRepository.findById(id)).thenReturn(Optional.of(quote));

            QuoteExpiredException ex = assertThrows(QuoteExpiredException.class, () ->
                    service.getActiveQuote(id, fixedNow));

            assertTrue(ex.getMessage().contains(id.toString()));
            assertTrue(ex.getMessage().contains(expiry.toString()));
        }

        @Test
        @DisplayName("getActiveQuote throws QuoteNotFoundException (not QuoteExpiredException) when ID is missing")
        void missingId_throwsNotFound_notExpired() {
            UUID id = UUID.randomUUID();
            when(priceQuoteRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(QuoteNotFoundException.class, () ->
                    service.getActiveQuote(id, fixedNow));
        }

        @Test
        @DisplayName("getActiveQuote (explicit) rejects null evaluationTime")
        void nullEvaluationTime_throwsIllegalArgumentException() {
            UUID id = UUID.randomUUID();
            assertThrows(IllegalArgumentException.class, () ->
                    service.getActiveQuote(id, null));
        }

        @Test
        @DisplayName("getActiveQuote (1-arg) evaluates using injected clock")
        void oneArgOverload_usesInjectedClock() {
            UUID id = UUID.randomUUID();
            PriceQuote activeQuote = validQuoteBuilder()
                    .id(id)
                    .expiresAt(fixedNow.plusMinutes(5))
                    .build();

            when(priceQuoteRepository.findById(id)).thenReturn(Optional.of(activeQuote));

            PriceQuote result = service.getActiveQuote(id);
            assertNotNull(result);
            assertEquals(id, result.getId());
        }

        @Test
        @DisplayName("getActiveQuoteDto returns mapped DTO for active quote")
        void getActiveQuoteDto_returnsMappedDto() {
            UUID id = UUID.randomUUID();
            PriceQuote activeQuote = validQuoteBuilder()
                    .id(id)
                    .expiresAt(fixedNow.plusMinutes(5))
                    .build();

            when(priceQuoteRepository.findById(id)).thenReturn(Optional.of(activeQuote));

            PricingResponseDto dto = service.getActiveQuoteDto(id);
            assertNotNull(dto);
            assertEquals(id, dto.getId());
            assertEquals(0, new BigDecimal("731.91").compareTo(dto.getFinalFare()));
        }
    }

    @Nested
    @DisplayName("DTO Mapping Integrity")
    class DtoMappingIntegrity {

        @Test
        @DisplayName("PriceQuoteMapper maps all 18 fields with scale 2 HALF_UP and updatedAt=null")
        void mapperNormalizesMonetaryFieldsAndNullsUpdatedAt() {
            UUID id = UUID.randomUUID();
            LocalDateTime createdAt = fixedNow.minusMinutes(5);
            LocalDateTime expiresAt = fixedNow.plusMinutes(25);

            PriceQuote quote = PriceQuote.builder()
                    .id(id)
                    .serviceType(ServiceType.EXPRESS)
                    .originZone("COLOMBO_CENTRAL")
                    .destinationZone("GREATER_COLOMBO")
                    .physicalWeightKg(new BigDecimal("2.555"))
                    .volumetricWeightKg(new BigDecimal("1.234"))
                    .billableWeightKg(new BigDecimal("2.555"))
                    .distanceKm(new BigDecimal("12.345"))
                    .baseFare(new BigDecimal("450.001"))
                    .weightCharge(new BigDecimal("150.125")) // HALF_UP -> 150.13
                    .distanceCharge(new BigDecimal("180.200"))
                    .urgencyMultiplier(new BigDecimal("1.2500"))
                    .zoneMultiplier(new BigDecimal("1.1000"))
                    .fuelSurchargeAmount(new BigDecimal("15.555")) // -> 15.56
                    .merchantDiscountAmount(new BigDecimal("20.125")) // -> 20.13
                    .promoDiscountAmount(new BigDecimal("50.004")) // -> 50.00
                    .finalFare(new BigDecimal("725.756")) // -> 725.76
                    .currency("LKR")
                    .expiresAt(expiresAt)
                    .createdAt(createdAt)
                    .build();

            PricingResponseDto dto = priceQuoteMapper.toDto(quote);

            assertEquals(id, dto.getId());
            assertEquals(ServiceType.EXPRESS, dto.getServiceType());
            assertEquals("COLOMBO_CENTRAL", dto.getOriginZone());
            assertEquals("GREATER_COLOMBO", dto.getDestinationZone());
            assertEquals(new BigDecimal("2.56"), dto.getPhysicalWeightKg());
            assertEquals(new BigDecimal("1.23"), dto.getVolumetricWeightKg());
            assertEquals(new BigDecimal("2.56"), dto.getBillableWeightKg());
            assertEquals(new BigDecimal("12.35"), dto.getDistanceKm());
            assertEquals(new BigDecimal("450.00"), dto.getBaseFare());
            assertEquals(new BigDecimal("150.13"), dto.getWeightCharge());
            assertEquals(new BigDecimal("180.20"), dto.getDistanceCharge());
            assertEquals(new BigDecimal("1.2500"), dto.getUrgencyMultiplier());
            assertEquals(new BigDecimal("1.1000"), dto.getZoneMultiplier());
            assertEquals(new BigDecimal("15.56"), dto.getFuelSurchargeAmount());
            assertEquals(new BigDecimal("20.13"), dto.getMerchantDiscountAmount());
            assertEquals(new BigDecimal("50.00"), dto.getPromoDiscountAmount());
            assertEquals(new BigDecimal("725.76"), dto.getFinalFare());
            assertEquals("LKR", dto.getCurrency());
            assertEquals(expiresAt, dto.getExpiresAt());
            assertEquals(createdAt, dto.getCreatedAt());
            assertNull(dto.getUpdatedAt());
        }

        @Test
        @DisplayName("PriceQuoteMapper throws IllegalArgumentException on null quote")
        void mapper_rejectsNullQuote() {
            assertThrows(IllegalArgumentException.class, () -> priceQuoteMapper.toDto(null));
        }
    }

    @Nested
    @DisplayName("Expired Quotes Query / Cleanup Helper")
    class ExpiredQuotesQuery {

        @Test
        @DisplayName("findExpiredQuotes delegates directly to repository.findByExpiresAtBefore")
        void delegatesToRepository() {
            LocalDateTime cutoff = fixedNow;
            PriceQuote quote1 = validQuoteBuilder().expiresAt(cutoff.minusMinutes(10)).build();
            when(priceQuoteRepository.findByExpiresAtBefore(cutoff)).thenReturn(List.of(quote1));

            List<PriceQuote> result = service.findExpiredQuotes(cutoff);
            assertEquals(1, result.size());
            verify(priceQuoteRepository, times(1)).findByExpiresAtBefore(cutoff);
        }
    }
}
