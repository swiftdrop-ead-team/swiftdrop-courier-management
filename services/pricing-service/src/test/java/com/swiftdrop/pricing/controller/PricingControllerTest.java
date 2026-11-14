package com.swiftdrop.pricing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.pricing.dto.PricingRequestDto;
import com.swiftdrop.pricing.dto.PricingResponseDto;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.exception.DeliveryRestrictionException;
import com.swiftdrop.pricing.exception.FuelRateNotFoundException;
import com.swiftdrop.pricing.exception.GlobalExceptionHandler;
import com.swiftdrop.pricing.exception.QuoteExpiredException;
import com.swiftdrop.pricing.exception.QuoteNotFoundException;
import com.swiftdrop.pricing.exception.ZoneNotFoundException;
import com.swiftdrop.pricing.service.PricingFacadeService;
import com.swiftdrop.pricing.security.JwtAuthenticationFilter;
import com.swiftdrop.pricing.security.JwtTokenProvider;
import com.swiftdrop.pricing.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({PricingController.class, GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
@WithMockUser(roles = "CUSTOMER")
class PricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PricingFacadeService pricingFacadeService;

    private PricingRequestDto createValidRequest() {
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

    private PricingResponseDto createSampleResponse(UUID id) {
        return PricingResponseDto.builder()
                .id(id)
                .serviceType(ServiceType.STANDARD)
                .originZone("COLOMBO_CENTRAL")
                .destinationZone("GREATER_COLOMBO")
                .physicalWeightKg(new BigDecimal("2.50"))
                .volumetricWeightKg(new BigDecimal("1.20"))
                .billableWeightKg(new BigDecimal("2.50"))
                .distanceKm(new BigDecimal("15.00"))
                .baseFare(new BigDecimal("350.00"))
                .weightCharge(new BigDecimal("187.50"))
                .distanceCharge(new BigDecimal("225.00"))
                .urgencyMultiplier(new BigDecimal("1.0000"))
                .zoneMultiplier(new BigDecimal("1.1500"))
                .fuelSurchargeAmount(new BigDecimal("12.38"))
                .merchantDiscountAmount(new BigDecimal("0.00"))
                .promoDiscountAmount(new BigDecimal("87.69"))
                .finalFare(new BigDecimal("801.57"))
                .currency("LKR")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .build();
    }

    @Test
    @DisplayName("POST /quotes - returns 201 Created with Location header and ApiResponse payload")
    void testCreateQuote_success() throws Exception {
        UUID quoteId = UUID.randomUUID();
        PricingRequestDto request = createValidRequest();
        PricingResponseDto response = createSampleResponse(quoteId);

        when(pricingFacadeService.calculateAndCreateQuote(any(PricingRequestDto.class), isNull()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/pricing/quotes/" + quoteId)))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Price quote created successfully")))
                .andExpect(jsonPath("$.data.id", is(quoteId.toString())))
                .andExpect(jsonPath("$.data.serviceType", is("STANDARD")))
                .andExpect(jsonPath("$.data.originZone", is("COLOMBO_CENTRAL")))
                .andExpect(jsonPath("$.data.destinationZone", is("GREATER_COLOMBO")))
                .andExpect(jsonPath("$.data.finalFare", is(801.57)))
                .andExpect(jsonPath("$.data.currency", is("LKR")))
                .andExpect(jsonPath("$.data.merchantDiscountAmount", is(0.0)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(pricingFacadeService).calculateAndCreateQuote(any(PricingRequestDto.class), isNull());
    }

    @Test
    @DisplayName("POST /quotes - returns 400 Bad Request with field error when serviceType is missing")
    void testCreateQuote_missingServiceType() throws Exception {
        PricingRequestDto request = createValidRequest();
        request.setServiceType(null);

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.data.serviceType", is("Service type is required")));

        verifyNoInteractions(pricingFacadeService);
    }

    @Test
    @DisplayName("POST /quotes - returns 400 Bad Request with field error when originZone is blank")
    void testCreateQuote_blankOriginZone() throws Exception {
        PricingRequestDto request = createValidRequest();
        request.setOriginZone("   ");

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.data.originZone", is("Origin zone is required")));

        verifyNoInteractions(pricingFacadeService);
    }

    @Test
    @DisplayName("POST /quotes - returns 400 Bad Request when physicalWeightKg is zero or negative")
    void testCreateQuote_invalidPhysicalWeight() throws Exception {
        PricingRequestDto request = createValidRequest();
        request.setPhysicalWeightKg(BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.data.physicalWeightKg", notNullValue()));

        verifyNoInteractions(pricingFacadeService);
    }

    @Test
    @DisplayName("POST /quotes - returns 400 Bad Request when dimensions are zero or negative")
    void testCreateQuote_invalidDimensions() throws Exception {
        PricingRequestDto request = createValidRequest();
        request.setLengthCm(new BigDecimal("-5.00"));

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.data.lengthCm", notNullValue()));

        verifyNoInteractions(pricingFacadeService);
    }

    @Test
    @DisplayName("POST /quotes - returns 400 Bad Request when distanceKm is negative")
    void testCreateQuote_negativeDistance() throws Exception {
        PricingRequestDto request = createValidRequest();
        request.setDistanceKm(new BigDecimal("-1.00"));

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.data.distanceKm", notNullValue()));

        verifyNoInteractions(pricingFacadeService);
    }

    @Test
    @DisplayName("POST /quotes - returns 400 Bad Request when originZone exceeds 50 characters")
    void testCreateQuote_originZoneExceedsMaxLength() throws Exception {
        PricingRequestDto request = createValidRequest();
        request.setOriginZone("A".repeat(51));

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.data.originZone", is("Origin zone must not exceed 50 characters")));

        verifyNoInteractions(pricingFacadeService);
    }

    @Test
    @DisplayName("POST /quotes - returns 400 Bad Request when physicalWeightKg exceeds maximum limit")
    void testCreateQuote_weightExceedsMaxBound() throws Exception {
        PricingRequestDto request = createValidRequest();
        request.setPhysicalWeightKg(new BigDecimal("1000000.00"));

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")));

        verifyNoInteractions(pricingFacadeService);
    }

    @Test
    @DisplayName("POST /quotes - returns 400 Bad Request when serviceType enum is unrecognized (malformed JSON)")
    void testCreateQuote_unrecognizedServiceType() throws Exception {
        String invalidJson = """
                {
                    "serviceType": "OVERNIGHT_AIR",
                    "originZone": "COLOMBO_CENTRAL",
                    "destinationZone": "GREATER_COLOMBO",
                    "physicalWeightKg": 2.50,
                    "lengthCm": 30.00,
                    "widthCm": 20.00,
                    "heightCm": 10.00,
                    "distanceKm": 15.00
                }
                """;

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Malformed request payload or unreadable JSON format")));

        verifyNoInteractions(pricingFacadeService);
    }

    @Test
    @DisplayName("POST /quotes - returns 404 Not Found when origin or destination zone does not exist")
    void testCreateQuote_zoneNotFound() throws Exception {
        PricingRequestDto request = createValidRequest();
        when(pricingFacadeService.calculateAndCreateQuote(any(), isNull()))
                .thenThrow(new ZoneNotFoundException("Zone not found: UNKNOWN_ZONE"));

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Zone not found: UNKNOWN_ZONE")));
    }

    @Test
    @DisplayName("POST /quotes - returns 400 Bad Request when delivery restrictions are violated")
    void testCreateQuote_deliveryRestriction() throws Exception {
        PricingRequestDto request = createValidRequest();
        when(pricingFacadeService.calculateAndCreateQuote(any(), isNull()))
                .thenThrow(new DeliveryRestrictionException("Package exceeds express maximum weight limit"));

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Package exceeds express maximum weight limit")));
    }

    @Test
    @DisplayName("POST /quotes - returns 500 Internal Server Error when fuel surcharge rate is missing")
    void testCreateQuote_fuelRateNotFound() throws Exception {
        PricingRequestDto request = createValidRequest();
        when(pricingFacadeService.calculateAndCreateQuote(any(), isNull()))
                .thenThrow(new FuelRateNotFoundException("No active rate"));

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Current fuel surcharge rate is not configured. Please contact support.")));
    }

    @Test
    @DisplayName("POST /quotes - returns 500 Internal Server Error with safe message for unexpected exceptions")
    void testCreateQuote_unexpectedException() throws Exception {
        PricingRequestDto request = createValidRequest();
        when(pricingFacadeService.calculateAndCreateQuote(any(), isNull()))
                .thenThrow(new RuntimeException("Database connection timeout or internal error"));

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("An unexpected internal error occurred. Please try again later.")));
    }

    @Test
    @DisplayName("GET /quotes/{id} - returns 200 OK with historical quote")
    void testGetQuote_success() throws Exception {
        UUID quoteId = UUID.randomUUID();
        PricingResponseDto response = createSampleResponse(quoteId);

        when(pricingFacadeService.getQuote(quoteId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/pricing/quotes/{id}", quoteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(quoteId.toString())))
                .andExpect(jsonPath("$.data.serviceType", is("STANDARD")))
                .andExpect(jsonPath("$.data.finalFare", is(801.57)));

        verify(pricingFacadeService).getQuote(quoteId);
    }

    @Test
    @DisplayName("GET /quotes/{id} - returns 404 Not Found when quote does not exist")
    void testGetQuote_notFound() throws Exception {
        UUID quoteId = UUID.randomUUID();
        when(pricingFacadeService.getQuote(quoteId)).thenThrow(new QuoteNotFoundException(quoteId));

        mockMvc.perform(get("/api/v1/pricing/quotes/{id}", quoteId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Price quote not found with ID: " + quoteId)));

        verify(pricingFacadeService).getQuote(quoteId);
    }

    @Test
    @DisplayName("GET /quotes/{id} - returns 400 Bad Request with parameter info when ID is not a valid UUID")
    void testGetQuote_invalidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/pricing/quotes/not-a-valid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid value 'not-a-valid-uuid' for parameter 'id'")));

        verifyNoInteractions(pricingFacadeService);
    }

    @Test
    @DisplayName("GET /quotes/{id}/active - returns 200 OK with active quote")
    void testGetActiveQuote_success() throws Exception {
        UUID quoteId = UUID.randomUUID();
        PricingResponseDto response = createSampleResponse(quoteId);

        when(pricingFacadeService.getActiveQuote(quoteId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/pricing/quotes/{id}/active", quoteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(quoteId.toString())))
                .andExpect(jsonPath("$.data.serviceType", is("STANDARD")))
                .andExpect(jsonPath("$.data.finalFare", is(801.57)));

        verify(pricingFacadeService).getActiveQuote(quoteId);
    }

    @Test
    @DisplayName("GET /quotes/{id}/active - returns 410 Gone when quote has expired")
    void testGetActiveQuote_expired() throws Exception {
        UUID quoteId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.of(2026, 11, 10, 10, 0, 0);
        when(pricingFacadeService.getActiveQuote(quoteId))
                .thenThrow(new QuoteExpiredException(quoteId, expiresAt));

        mockMvc.perform(get("/api/v1/pricing/quotes/{id}/active", quoteId))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("expired")));

        verify(pricingFacadeService).getActiveQuote(quoteId);
    }

    @Test
    @DisplayName("GET /quotes/{id}/active - returns 400 Bad Request when ID is not a valid UUID")
    void testGetActiveQuote_invalidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/pricing/quotes/invalid-uuid/active"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid value 'invalid-uuid' for parameter 'id'")));

        verifyNoInteractions(pricingFacadeService);
    }
}
