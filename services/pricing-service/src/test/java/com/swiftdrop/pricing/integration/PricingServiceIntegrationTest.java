package com.swiftdrop.pricing.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.pricing.dto.PricingRequestDto;
import com.swiftdrop.pricing.entity.*;
import com.swiftdrop.pricing.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test verifying that the Pricing Service REST API,
 * facade orchestration, pricing strategies, zone & fuel calculators, discount engine,
 * database persistence, and global exception handling operate seamlessly together.
 *
 * Uses the live Spring application context with the H2 in-memory database.
 * No service beans or repositories are mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PricingServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    @Autowired
    private DeliveryZoneRepository deliveryZoneRepository;

    @Autowired
    private FuelSurchargeIndexRepository fuelSurchargeIndexRepository;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @Autowired
    private PriceQuoteRepository priceQuoteRepository;

    @BeforeEach
    void setUpReferenceData() {
        // Clean up transactional data first, then reference tables
        priceQuoteRepository.deleteAll();
        pricingRuleRepository.deleteAll();
        deliveryZoneRepository.deleteAll();
        fuelSurchargeIndexRepository.deleteAll();
        promoCodeRepository.deleteAll();

        // 1. Seed Standard Pricing Rule
        PricingRule standardRule = PricingRule.builder()
                .serviceType(ServiceType.STANDARD)
                .baseFare(new BigDecimal("350.00"))
                .baseWeightKg(new BigDecimal("1.00"))
                .perKgRate(new BigDecimal("75.00"))
                .perKmRate(new BigDecimal("15.00"))
                .volumetricDivisor(5000)
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .isActive(true)
                .build();
        pricingRuleRepository.save(standardRule);

        // 2. Seed Express Pricing Rule (with 30kg max limit in ExpressDeliveryPricingStrategy)
        PricingRule expressRule = PricingRule.builder()
                .serviceType(ServiceType.EXPRESS)
                .baseFare(new BigDecimal("500.00"))
                .baseWeightKg(new BigDecimal("1.00"))
                .perKgRate(new BigDecimal("100.00"))
                .perKmRate(new BigDecimal("25.00"))
                .volumetricDivisor(5000)
                .urgencyMultiplier(new BigDecimal("1.2500"))
                .isActive(true)
                .build();
        pricingRuleRepository.save(expressRule);

        // 3. Seed Origin Zone (Active, multiplier 1.0)
        DeliveryZone originZone = DeliveryZone.builder()
                .zoneCode("COLOMBO_CENTRAL")
                .zoneName("Colombo Central")
                .baseZoneMultiplier(new BigDecimal("1.0000"))
                .isActive(true)
                .build();
        deliveryZoneRepository.save(originZone);

        // 4. Seed Destination Zone (Active, multiplier 1.15)
        DeliveryZone destZone = DeliveryZone.builder()
                .zoneCode("GREATER_COLOMBO")
                .zoneName("Greater Colombo Suburbs")
                .baseZoneMultiplier(new BigDecimal("1.1500"))
                .isActive(true)
                .build();
        deliveryZoneRepository.save(destZone);

        // 5. Seed Inactive Zone
        DeliveryZone inactiveZone = DeliveryZone.builder()
                .zoneCode("INACTIVE_ZONE")
                .zoneName("Discontinued Zone")
                .baseZoneMultiplier(new BigDecimal("1.5000"))
                .isActive(false)
                .build();
        deliveryZoneRepository.save(inactiveZone);

        // 6. Seed Active Fuel Surcharge Index (effective in past)
        FuelSurchargeIndex fuelIndex = FuelSurchargeIndex.builder()
                .ratePercentage(new BigDecimal("5.50"))
                .effectiveFrom(LocalDateTime.now().minusDays(10))
                .isActive(true)
                .build();
        fuelSurchargeIndexRepository.save(fuelIndex);

        // 7. Seed Active Promo Code
        PromoCode promo = PromoCode.builder()
                .code("WELCOME10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .minOrderAmount(new BigDecimal("100.00"))
                .validFrom(LocalDateTime.now().minusDays(5))
                .validTo(LocalDateTime.now().plusDays(30))
                .isActive(true)
                .timesUsed(0)
                .build();
        promoCodeRepository.save(promo);
    }

    private PricingRequestDto createValidStandardRequest() {
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
                .orderReference("ORD-INTEG-001")
                .customerId("CUST-INTEG-99")
                .build();
    }

    @Test
    @DisplayName("Complete Quote Lifecycle: POST calculate -> verify DB -> GET historical -> GET active -> expire -> 410 Gone")
    void testCompleteQuoteLifecycle() throws Exception {
        PricingRequestDto request = createValidStandardRequest();

        // 1. Submit valid pricing request (POST /api/v1/pricing/quotes)
        MvcResult postResult = mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/pricing/quotes/")))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Price quote created successfully")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.serviceType", is("STANDARD")))
                .andExpect(jsonPath("$.data.currency", is("LKR")))
                .andExpect(jsonPath("$.data.finalFare", notNullValue()))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(postResult.getResponse().getContentAsString());
        UUID quoteId = UUID.fromString(responseNode.path("data").path("id").asText());
        BigDecimal calculatedFinalFare = new BigDecimal(responseNode.path("data").path("finalFare").asText());

        // 2. Verify quote was persisted in the actual database
        Optional<PriceQuote> persistedOpt = priceQuoteRepository.findById(quoteId);
        assertTrue(persistedOpt.isPresent(), "Price quote must exist in database");
        PriceQuote persisted = persistedOpt.get();
        assertEquals(ServiceType.STANDARD, persisted.getServiceType());
        assertEquals("COLOMBO_CENTRAL", persisted.getOriginZone());
        assertEquals("GREATER_COLOMBO", persisted.getDestinationZone());
        assertEquals("LKR", persisted.getCurrency());
        assertEquals(0, calculatedFinalFare.compareTo(persisted.getFinalFare()));
        assertNotNull(persisted.getCreatedAt());
        assertTrue(persisted.getExpiresAt().isAfter(LocalDateTime.now()));

        // 3. Retrieve historical quote (GET /api/v1/pricing/quotes/{id})
        mockMvc.perform(get("/api/v1/pricing/quotes/{id}", quoteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(quoteId.toString())))
                .andExpect(jsonPath("$.data.serviceType", is("STANDARD")))
                .andExpect(jsonPath("$.data.currency", is("LKR")));

        // 4. Retrieve active quote (GET /api/v1/pricing/quotes/{id}/active)
        mockMvc.perform(get("/api/v1/pricing/quotes/{id}/active", quoteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(quoteId.toString())));

        // 5. Expire the quote directly in database (expiresAt set to 10 minutes ago)
        persisted.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        priceQuoteRepository.saveAndFlush(persisted);

        // 6. Active retrieval now returns 410 Gone with ApiResponse error
        mockMvc.perform(get("/api/v1/pricing/quotes/{id}/active", quoteId))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("expired")));

        // 7. Historical retrieval still returns 200 OK
        mockMvc.perform(get("/api/v1/pricing/quotes/{id}", quoteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(quoteId.toString())));
    }

    @Test
    @DisplayName("Domain Failure: Unknown origin zone returns 404 Not Found")
    void testFailure_unknownZone() throws Exception {
        PricingRequestDto request = createValidStandardRequest();
        request.setOriginZone("NON_EXISTENT_ZONE");

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Delivery zone not found with code: NON_EXISTENT_ZONE")));
    }

    @Test
    @DisplayName("Domain Failure: Inactive zone returns 400 Bad Request")
    void testFailure_inactiveZone() throws Exception {
        PricingRequestDto request = createValidStandardRequest();
        request.setOriginZone("INACTIVE_ZONE");

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Delivery zone is inactive: INACTIVE_ZONE")));
    }

    @Test
    @DisplayName("Domain Failure: Non-existent promo code returns 400 Bad Request")
    void testFailure_invalidPromoCode() throws Exception {
        PricingRequestDto request = createValidStandardRequest();
        request.setPromoCode("FAKE_PROMO_CODE");

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Promo code not found: FAKE_PROMO_CODE")));
    }

    @Test
    @DisplayName("Domain Failure: Express delivery exceeding 30kg package weight cap returns 400 Bad Request")
    void testFailure_deliveryRestriction() throws Exception {
        PricingRequestDto request = createValidStandardRequest();
        request.setServiceType(ServiceType.EXPRESS);
        request.setPhysicalWeightKg(new BigDecimal("35.00")); // Exceeds 30.00 kg Express limit

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("exceeds express maximum weight limit of 30.00 kg")));
    }

    @Test
    @DisplayName("Concurrency: 10 simultaneous quote calculations produce unique IDs and clean persistence")
    void testConcurrentQuoteCreation() throws Exception {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Future<UUID>> futures = new ArrayList<>();
        Set<UUID> generatedIds = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executorService.submit(() -> {
                startLatch.await(); // Synchronize all threads to start concurrently
                try {
                    PricingRequestDto request = createValidStandardRequest();
                    request.setOrderReference("ORD-CONC-" + index);

                    MvcResult result = mockMvc.perform(post("/api/v1/pricing/quotes")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isCreated())
                            .andReturn();

                    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
                    UUID id = UUID.fromString(json.path("data").path("id").asText());
                    generatedIds.add(id);
                    return id;
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        startLatch.countDown(); // Unleash all 10 threads simultaneously
        boolean completedInTime = doneLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        assertTrue(completedInTime, "All 10 concurrent requests should complete within 30 seconds");

        // Verify all 10 futures succeeded without exception
        for (Future<UUID> future : futures) {
            assertNotNull(future.get());
        }

        // Verify exactly 10 unique UUIDs were generated (no collisions)
        assertEquals(threadCount, generatedIds.size(), "Each concurrent quote must have a distinct UUID");

        // Verify all 10 quotes are persisted in the database
        List<PriceQuote> persistedQuotes = priceQuoteRepository.findAllById(generatedIds);
        assertEquals(threadCount, persistedQuotes.size(), "All 10 quotes must be persisted in database");
    }
}
