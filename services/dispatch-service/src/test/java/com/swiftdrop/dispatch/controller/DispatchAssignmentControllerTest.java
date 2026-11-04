package com.swiftdrop.dispatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.dispatch.dto.*;
import com.swiftdrop.dispatch.entity.AssignmentStatus;
import com.swiftdrop.dispatch.exception.GlobalExceptionHandler;
import com.swiftdrop.dispatch.service.DispatchAssignmentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DispatchAssignmentControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DispatchAssignmentService assignmentService;

    @InjectMocks
    private DispatchAssignmentController assignmentController;

    private DispatchAssignmentResponse sampleAssignment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(assignmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleAssignment = DispatchAssignmentResponse.builder()
                .id(100L)
                .parcelId("p-123")
                .courierId(1L)
                .courierName("Kasun Perera")
                .trackingNumber("SWIFT-001")
                .status(AssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/dispatch/assignments - Should create assignment")
    void shouldCreateAssignment() throws Exception {
        AssignCourierRequest request = AssignCourierRequest.builder()
                .parcelId("p-123")
                .courierId(1L)
                .trackingNumber("SWIFT-001")
                .build();

        when(assignmentService.assignCourier(any(AssignCourierRequest.class))).thenReturn(sampleAssignment);

        mockMvc.perform(post("/api/v1/dispatch/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingNumber").value("SWIFT-001"));
    }

    @Test
    @DisplayName("PATCH /api/v1/dispatch/assignments/{id}/status - Should update delivery status")
    void shouldUpdateStatus() throws Exception {
        UpdateAssignmentStatusRequest request = UpdateAssignmentStatusRequest.builder()
                .status(AssignmentStatus.IN_TRANSIT)
                .build();

        sampleAssignment.setStatus(AssignmentStatus.IN_TRANSIT);
        when(assignmentService.updateAssignmentStatus(eq(100L), any(UpdateAssignmentStatusRequest.class)))
                .thenReturn(sampleAssignment);

        mockMvc.perform(patch("/api/v1/dispatch/assignments/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_TRANSIT"));
    }

    @Test
    @DisplayName("POST /api/v1/dispatch/assignments/{id}/proof - Should record Proof of Delivery")
    void shouldRecordProofOfDelivery() throws Exception {
        ProofOfDeliveryRequest request = ProofOfDeliveryRequest.builder()
                .recipientName("Kamal Gunaratne")
                .recipientNic("951234567V")
                .build();

        ProofOfDeliveryResponse podResponse = ProofOfDeliveryResponse.builder()
                .id(50L)
                .assignmentId(100L)
                .recipientName("Kamal Gunaratne")
                .deliveredAt(LocalDateTime.now())
                .build();

        when(assignmentService.submitProofOfDelivery(eq(100L), any(ProofOfDeliveryRequest.class)))
                .thenReturn(podResponse);

        mockMvc.perform(post("/api/v1/dispatch/assignments/100/proof")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recipientName").value("Kamal Gunaratne"));
    }
}

