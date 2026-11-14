package com.swiftdrop.parcel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.parcel.config.SecurityConfig;
import com.swiftdrop.parcel.dto.request.CancelParcelRequest;
import com.swiftdrop.parcel.dto.request.CreateParcelRequest;
import com.swiftdrop.parcel.dto.request.UpdateParcelStatusRequest;
import com.swiftdrop.parcel.dto.response.ParcelResponse;
import com.swiftdrop.parcel.dto.response.PublicTrackingResponse;
import com.swiftdrop.parcel.enums.DeliveryType;
import com.swiftdrop.parcel.enums.ParcelStatus;
import com.swiftdrop.parcel.enums.SenderType;
import com.swiftdrop.parcel.security.JwtAuthenticationFilter;
import com.swiftdrop.parcel.security.JwtService;
import com.swiftdrop.parcel.security.UserPrincipal;
import com.swiftdrop.parcel.service.ParcelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParcelController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class ParcelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ParcelService parcelService;

    @MockitoBean
    private JwtService jwtService;

    private UserPrincipal customerPrincipal;
    private UsernamePasswordAuthenticationToken customerAuth;

    private UserPrincipal courierPrincipal;
    private UsernamePasswordAuthenticationToken courierAuth;

    @BeforeEach
    void setUp() {
        customerPrincipal = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .email("customer@swiftdrop.io")
                .fullName("Alice Customer")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();
        customerAuth = new UsernamePasswordAuthenticationToken(
                customerPrincipal, null, customerPrincipal.getAuthorities());

        courierPrincipal = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .email("courier@swiftdrop.io")
                .fullName("Bob Courier")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_COURIER")))
                .build();
        courierAuth = new UsernamePasswordAuthenticationToken(
                courierPrincipal, null, courierPrincipal.getAuthorities());
    }

    @Test
    @DisplayName("GET /api/v1/parcels/tracking/{trackingNumber} should be accessible publicly without auth")
    void testPublicTrackingEndpointOpen() throws Exception {
        PublicTrackingResponse response = PublicTrackingResponse.builder()
                .trackingNumber("SD-1234-5678-LK")
                .status(ParcelStatus.IN_TRANSIT)
                .deliveryType(DeliveryType.STANDARD)
                .originCity("Colombo")
                .destinationCity("Kandy")
                .isFragile(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .checkpoints(Collections.emptyList())
                .build();

        when(parcelService.getPublicTracking("SD-1234-5678-LK")).thenReturn(response);

        mockMvc.perform(get("/api/v1/parcels/tracking/SD-1234-5678-LK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value("SD-1234-5678-LK"))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.originCity").value("Colombo"))
                .andExpect(jsonPath("$.destinationCity").value("Kandy"));
    }

    @Test
    @DisplayName("POST /api/v1/parcels should create parcel when payload is valid")
    void testCreateParcelSuccess() throws Exception {
        CreateParcelRequest request = CreateParcelRequest.builder()
                .senderName("Alice Doe")
                .senderPhone("+94771234567")
                .senderEmail("alice@example.com")
                .pickupAddress("10 Galle Road")
                .pickupCity("Colombo")
                .pickupPostalCode("00300")
                .recipientName("Bob Dylan")
                .recipientPhone("+94779876543")
                .recipientEmail("bob@example.com")
                .deliveryAddress("20 Peradeniya Road")
                .deliveryCity("Kandy")
                .deliveryPostalCode("20000")
                .weightKg(new BigDecimal("1.50"))
                .lengthCm(new BigDecimal("15.00"))
                .widthCm(new BigDecimal("10.00"))
                .heightCm(new BigDecimal("5.00"))
                .isFragile(false)
                .deliveryType(DeliveryType.STANDARD)
                .build();

        UUID parcelId = UUID.randomUUID();
        ParcelResponse response = ParcelResponse.builder()
                .id(parcelId)
                .trackingNumber("SD-5555-6666-LK")
                .senderType(SenderType.CUSTOMER)
                .senderName("Alice Doe")
                .status(ParcelStatus.CREATED)
                .deliveryFee(new BigDecimal("12.50"))
                .currency("USD")
                .build();

        when(parcelService.createParcel(any(CreateParcelRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/parcels")
                        .with(authentication(customerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(parcelId.toString()))
                .andExpect(jsonPath("$.trackingNumber").value("SD-5555-6666-LK"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("POST /api/v1/parcels should return 400 Bad Request when validation constraints are violated")
    void testCreateParcelValidationFailure() throws Exception {
        CreateParcelRequest invalidRequest = CreateParcelRequest.builder()
                .recipientName("") // Blank recipient name
                .deliveryAddress("") // Blank delivery address
                .weightKg(new BigDecimal("-5.00")) // Negative weight
                .build();

        mockMvc.perform(post("/api/v1/parcels")
                        .with(authentication(customerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("PATCH /api/v1/parcels/{id}/status should update parcel status when invoked by Courier")
    void testUpdateStatusAsCourier() throws Exception {
        UUID parcelId = UUID.randomUUID();
        UpdateParcelStatusRequest request = UpdateParcelStatusRequest.builder()
                .newStatus(ParcelStatus.PICKED_UP)
                .location("Central Sorting Hub")
                .notes("Picked up successfully")
                .build();

        ParcelResponse response = ParcelResponse.builder()
                .id(parcelId)
                .trackingNumber("SD-1111-2222-LK")
                .status(ParcelStatus.PICKED_UP)
                .build();

        when(parcelService.updateParcelStatus(eq(parcelId), any(UpdateParcelStatusRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/parcels/" + parcelId + "/status")
                        .with(authentication(courierAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));
    }

    @Test
    @DisplayName("POST /api/v1/parcels/{id}/cancel should cancel parcel")
    void testCancelParcelAsCustomer() throws Exception {
        UUID parcelId = UUID.randomUUID();
        CancelParcelRequest request = CancelParcelRequest.builder()
                .reason("Address error")
                .build();

        ParcelResponse response = ParcelResponse.builder()
                .id(parcelId)
                .trackingNumber("SD-1111-2222-LK")
                .status(ParcelStatus.CANCELLED)
                .build();

        when(parcelService.cancelParcel(eq(parcelId), any(CancelParcelRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/parcels/" + parcelId + "/cancel")
                        .with(authentication(customerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Protected endpoint should return 401 Unauthorized when request has no token")
    void testProtectedEndpointWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/parcels/my-parcels"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
