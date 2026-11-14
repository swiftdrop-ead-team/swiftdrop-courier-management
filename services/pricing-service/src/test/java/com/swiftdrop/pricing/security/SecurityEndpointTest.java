package com.swiftdrop.pricing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.pricing.dto.PricingRequestDto;
import com.swiftdrop.pricing.dto.PricingResponseDto;
import com.swiftdrop.pricing.entity.ServiceType;
import com.swiftdrop.pricing.service.PricingFacadeService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Endpoint Access and Authorization Tests")
class SecurityEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PricingFacadeService pricingFacadeService;

    private static final String OTHER_SECRET = "dGhpc0lzQURpZmZlcmVudFNlY3JldEtleUZvclRlc3Rpbmc4OTAxMjM0NTY3ODkwMTI=";

    @Test
    @DisplayName("Public endpoint /api/v1/pricing/health is accessible without authentication")
    void publicHealthEndpointAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/pricing/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.service", is("pricing-service")))
                .andExpect(jsonPath("$.data.status", is("UP")));
    }

    @Test
    @DisplayName("Public actuator /actuator/health is accessible without authentication")
    void publicActuatorHealthAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quotes without token returns 401 Unauthorized")
    void createQuoteWithoutTokenReturns401() throws Exception {
        PricingRequestDto dto = createValidRequestDto();

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("GET /api/v1/pricing/quotes/{id} without token returns 401 Unauthorized")
    void getQuoteWithoutTokenReturns401() throws Exception {
        UUID quoteId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/pricing/quotes/" + quoteId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/pricing/quotes/{id}/active without token returns 401 Unauthorized")
    void getActiveQuoteWithoutTokenReturns401() throws Exception {
        UUID quoteId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/pricing/quotes/" + quoteId + "/active"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quotes with invalid signature returns 401 Unauthorized")
    void createQuoteWithInvalidSignatureReturns401() throws Exception {
        byte[] keyBytes = Decoders.BASE64.decode(OTHER_SECRET);
        SecretKey otherKey = Keys.hmacShaKeyFor(keyBytes);

        String invalidSignatureToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "hacker@swiftdrop.io")
                .claim("roles", List.of("ROLE_CUSTOMER"))
                .issuer(JwtTokenProvider.ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(otherKey)
                .compact();

        PricingRequestDto dto = createValidRequestDto();

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .header("Authorization", "Bearer " + invalidSignatureToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quotes with expired token returns 401 Unauthorized")
    void createQuoteWithExpiredTokenReturns401() throws Exception {
        String expiredToken = jwtTokenProvider.generateToken(
                UUID.randomUUID(), "expired@swiftdrop.io", "Expired User",
                List.of("ROLE_CUSTOMER"), -5000);

        PricingRequestDto dto = createValidRequestDto();

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quotes with malformed token returns 401 Unauthorized")
    void createQuoteWithMalformedTokenReturns401() throws Exception {
        PricingRequestDto dto = createValidRequestDto();

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .header("Authorization", "Bearer not-a-valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quotes with unauthorized role (e.g. ROLE_COURIER) returns 403 Forbidden")
    void createQuoteWithForbiddenRoleReturns403() throws Exception {
        String courierToken = jwtTokenProvider.generateToken(
                UUID.randomUUID(), "courier@swiftdrop.io", "Courier Driver",
                List.of("ROLE_COURIER"), 60000);

        PricingRequestDto dto = createValidRequestDto();

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .header("Authorization", "Bearer " + courierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("You do not have permission to access this resource")));
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quotes with valid CUSTOMER token succeeds")
    void createQuoteWithValidCustomerTokenSucceeds() throws Exception {
        UUID quoteId = UUID.randomUUID();
        PricingResponseDto responseDto = PricingResponseDto.builder()
                .id(quoteId)
                .serviceType(ServiceType.STANDARD)
                .finalFare(new BigDecimal("1250.00"))
                .currency("LKR")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(pricingFacadeService.calculateAndCreateQuote(any(), isNull())).thenReturn(responseDto);

        String customerToken = jwtTokenProvider.generateToken(
                UUID.randomUUID(), "customer@swiftdrop.io", "Jane Customer",
                List.of("ROLE_CUSTOMER"), 60000);

        PricingRequestDto dto = createValidRequestDto();

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(quoteId.toString())));
    }

    @Test
    @DisplayName("POST /api/v1/pricing/quotes with valid MERCHANT token succeeds")
    void createQuoteWithValidMerchantTokenSucceeds() throws Exception {
        UUID quoteId = UUID.randomUUID();
        PricingResponseDto responseDto = PricingResponseDto.builder()
                .id(quoteId)
                .serviceType(ServiceType.STANDARD)
                .finalFare(new BigDecimal("1100.00"))
                .currency("LKR")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(pricingFacadeService.calculateAndCreateQuote(any(), isNull())).thenReturn(responseDto);

        String merchantToken = jwtTokenProvider.generateToken(
                UUID.randomUUID(), "merchant@swiftdrop.io", "Apex Merchant",
                List.of("ROLE_MERCHANT"), 60000);

        PricingRequestDto dto = createValidRequestDto();

        mockMvc.perform(post("/api/v1/pricing/quotes")
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/pricing/quotes/{id} with valid ADMIN token succeeds")
    void getQuoteWithValidAdminTokenSucceeds() throws Exception {
        UUID quoteId = UUID.randomUUID();
        PricingResponseDto responseDto = PricingResponseDto.builder()
                .id(quoteId)
                .serviceType(ServiceType.STANDARD)
                .finalFare(new BigDecimal("1250.00"))
                .currency("LKR")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(pricingFacadeService.getQuote(quoteId)).thenReturn(responseDto);

        String adminToken = jwtTokenProvider.generateToken(
                UUID.randomUUID(), "admin@swiftdrop.io", "System Admin",
                List.of("ROLE_ADMIN"), 60000);

        mockMvc.perform(get("/api/v1/pricing/quotes/" + quoteId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(quoteId.toString())));
    }

    private PricingRequestDto createValidRequestDto() {
        PricingRequestDto dto = new PricingRequestDto();
        dto.setServiceType(ServiceType.STANDARD);
        dto.setOriginZone("WESTERN");
        dto.setDestinationZone("CENTRAL");
        dto.setPhysicalWeightKg(new BigDecimal("2.50"));
        dto.setLengthCm(new BigDecimal("20.00"));
        dto.setWidthCm(new BigDecimal("15.00"));
        dto.setHeightCm(new BigDecimal("10.00"));
        dto.setDistanceKm(new BigDecimal("35.00"));
        return dto;
    }
}
