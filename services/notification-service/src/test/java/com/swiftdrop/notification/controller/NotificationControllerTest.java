package com.swiftdrop.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.notification.dto.SendNotificationRequest;
import com.swiftdrop.notification.entity.NotificationChannel;
import com.swiftdrop.notification.entity.NotificationLog;
import com.swiftdrop.notification.entity.NotificationStatus;
import com.swiftdrop.notification.entity.NotificationType;
import com.swiftdrop.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @Test
    @DisplayName("GET /api/v1/notifications/history should return 200 with list of notifications")
    void shouldReturnNotificationHistory() throws Exception {
        NotificationLog mockLog = NotificationLog.builder()
                .id(1L)
                .parcelId("PRC-100")
                .trackingNumber("SWIFT-100")
                .recipientEmail("customer@swiftdrop.io")
                .recipientName("Alice")
                .notificationType(NotificationType.ORDER_CONFIRMED)
                .channel(NotificationChannel.EMAIL)
                .deliveryStatus(NotificationStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationService.getAllHistory()).thenReturn(List.of(mockLog));

        mockMvc.perform(get("/api/v1/notifications/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].parcelId").value("PRC-100"))
                .andExpect(jsonPath("$.data[0].recipientEmail").value("customer@swiftdrop.io"));
    }

    @Test
    @DisplayName("POST /api/v1/notifications/send should return 201 when valid request provided")
    void shouldSendManualNotification() throws Exception {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .parcelId("PRC-200")
                .trackingNumber("SWIFT-200")
                .recipientEmail("bob@swiftdrop.io")
                .recipientName("Bob")
                .notificationType(NotificationType.OUT_FOR_DELIVERY)
                .build();

        NotificationLog mockResult = NotificationLog.builder()
                .id(2L)
                .parcelId("PRC-200")
                .trackingNumber("SWIFT-200")
                .recipientEmail("bob@swiftdrop.io")
                .notificationType(NotificationType.OUT_FOR_DELIVERY)
                .deliveryStatus(NotificationStatus.SENT)
                .build();

        when(notificationService.sendManualNotification(any(), any(), any(), any(), any(), any()))
                .thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.parcelId").value("PRC-200"));
    }
}

