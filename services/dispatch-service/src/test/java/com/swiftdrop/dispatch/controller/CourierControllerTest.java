package com.swiftdrop.dispatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.dispatch.dto.*;
import com.swiftdrop.dispatch.entity.CourierStatus;
import com.swiftdrop.dispatch.entity.VehicleType;
import com.swiftdrop.dispatch.exception.GlobalExceptionHandler;
import com.swiftdrop.dispatch.service.CourierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourierControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CourierService courierService;

    @InjectMocks
    private CourierController courierController;

    private CourierResponse sampleCourier;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courierController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleCourier = CourierResponse.builder()
                .id(1L)
                .userId("courier-001")
                .fullName("Kasun Perera")
                .phoneNumber("+94771234567")
                .vehicleType(VehicleType.MOTORBIKE)
                .assignedZone("COLOMBO_CENTRAL")
                .status(CourierStatus.AVAILABLE)
                .maxActiveDeliveries(5)
                .currentActiveDeliveries(1)
                .availableForAssignment(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/dispatch/couriers - Should register courier")
    void shouldRegisterCourier() throws Exception {
        CourierCreateRequest request = CourierCreateRequest.builder()
                .userId("courier-001")
                .fullName("Kasun Perera")
                .phoneNumber("+94771234567")
                .vehicleType(VehicleType.MOTORBIKE)
                .build();

        when(courierService.registerCourier(any(CourierCreateRequest.class))).thenReturn(sampleCourier);

        mockMvc.perform(post("/api/v1/dispatch/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Kasun Perera"));
    }

    @Test
    @DisplayName("GET /api/v1/dispatch/couriers/workloads - Should return workload list")
    void shouldReturnWorkloads() throws Exception {
        CourierWorkloadResponse workload = CourierWorkloadResponse.builder()
                .courierId(1L)
                .courierName("Kasun Perera")
                .activeDeliveries(2)
                .maxDeliveries(5)
                .utilizationPercentage(40.0)
                .canAcceptMore(true)
                .build();

        when(courierService.getCourierWorkloads()).thenReturn(List.of(workload));

        mockMvc.perform(get("/api/v1/dispatch/couriers/workloads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].courierName").value("Kasun Perera"))
                .andExpect(jsonPath("$.data[0].utilizationPercentage").value(40.0));
    }

    @Test
    @DisplayName("PATCH /api/v1/dispatch/couriers/{id}/status - Should update status")
    void shouldUpdateStatus() throws Exception {
        CourierStatusUpdateRequest request = CourierStatusUpdateRequest.builder()
                .status(CourierStatus.OFFLINE)
                .build();

        sampleCourier.setStatus(CourierStatus.OFFLINE);
        when(courierService.updateCourierStatus(eq(1L), any(CourierStatusUpdateRequest.class)))
                .thenReturn(sampleCourier);

        mockMvc.perform(patch("/api/v1/dispatch/couriers/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFFLINE"));
    }
}

