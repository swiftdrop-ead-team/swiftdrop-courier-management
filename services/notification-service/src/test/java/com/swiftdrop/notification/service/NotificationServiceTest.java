package com.swiftdrop.notification.service;

import com.swiftdrop.notification.entity.NotificationLog;
import com.swiftdrop.notification.entity.NotificationStatus;
import com.swiftdrop.notification.entity.NotificationType;
import com.swiftdrop.notification.event.DeliveryStatusUpdatedEvent;
import com.swiftdrop.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @org.mockito.Mock private com.swiftdrop.notification.repository.EmailPreferenceRepository preferences;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        lenient().when(emailSenderService.sendHtmlEmail(anyString(), anyString(), anyString())).thenReturn(true);
        lenient().when(emailTemplateService.buildOutForDeliveryEmail(any(), any(), any(), any()))
                .thenReturn("<html>Out for delivery</html>");
        lenient().when(emailTemplateService.buildDeliveredEmail(any(), any(), any()))
                .thenReturn("<html>Delivered</html>");
        lenient().when(emailTemplateService.buildDeliveryFailedEmail(any(), any(), any(), any()))
                .thenReturn("<html>Failed</html>");
    }

    @Test
    @DisplayName("Should successfully send email and record log when parcel is OUT_FOR_DELIVERY")
    void shouldHandleOutForDeliveryEvent() {
        DeliveryStatusUpdatedEvent event = DeliveryStatusUpdatedEvent.builder()
                .eventId("evt-101")
                .parcelId("PRC-9901")
                .trackingNumber("SWIFT-9901")
                .newStatus("OUT_FOR_DELIVERY")
                .courierName("Dave Courier")
                .recipientEmail("alice@example.com")
                .recipientName("Alice Fernando")
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationLogRepository.existsByEventId("evt-101")).thenReturn(false);
        when(notificationLogRepository.existsByParcelIdAndNotificationTypeAndDeliveryStatus(
                "PRC-9901", NotificationType.OUT_FOR_DELIVERY, NotificationStatus.SENT)).thenReturn(false);

        notificationService.handleDeliveryStatusUpdated(event);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());

        NotificationLog saved = captor.getValue();
        assertEquals("PRC-9901", saved.getParcelId());
        assertEquals("alice@example.com", saved.getRecipientEmail());
        assertEquals(NotificationType.OUT_FOR_DELIVERY, saved.getNotificationType());
        assertEquals(NotificationStatus.SENT, saved.getDeliveryStatus());
    }

    @Test
    @DisplayName("Should prevent duplicate email when same event ID was already processed (Idempotency)")
    void shouldPreventDuplicateByEventId() {
        DeliveryStatusUpdatedEvent event = DeliveryStatusUpdatedEvent.builder()
                .eventId("evt-duplicate")
                .parcelId("PRC-9902")
                .newStatus("OUT_FOR_DELIVERY")
                .build();

        when(notificationLogRepository.existsByEventId("evt-duplicate")).thenReturn(true);

        notificationService.handleDeliveryStatusUpdated(event);

        verify(emailSenderService, never()).sendHtmlEmail(anyString(), anyString(), anyString());
        verify(notificationLogRepository, never()).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should prevent sending duplicate milestone email if already delivered to customer")
    void shouldPreventDuplicateMilestoneEmail() {
        DeliveryStatusUpdatedEvent event = DeliveryStatusUpdatedEvent.builder()
                .eventId("evt-different-id")
                .parcelId("PRC-9903")
                .newStatus("DELIVERED")
                .build();

        when(notificationLogRepository.existsByEventId("evt-different-id")).thenReturn(false);
        when(notificationLogRepository.existsByParcelIdAndNotificationTypeAndDeliveryStatus(
                "PRC-9903", NotificationType.DELIVERED, NotificationStatus.SENT)).thenReturn(true);

        notificationService.handleDeliveryStatusUpdated(event);

        verify(emailSenderService, never()).sendHtmlEmail(anyString(), anyString(), anyString());
        verify(notificationLogRepository, never()).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should skip internal status scans like IN_TRANSIT to avoid customer inbox spam")
    void shouldSkipInTransitStatus() {
        DeliveryStatusUpdatedEvent event = DeliveryStatusUpdatedEvent.builder()
                .parcelId("PRC-9904")
                .newStatus("IN_TRANSIT")
                .build();

        notificationService.handleDeliveryStatusUpdated(event);

        verify(emailSenderService, never()).sendHtmlEmail(anyString(), anyString(), anyString());
        verify(notificationLogRepository, never()).save(any(NotificationLog.class));
    }
}

