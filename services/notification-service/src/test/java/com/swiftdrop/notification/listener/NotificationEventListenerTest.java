package com.swiftdrop.notification.listener;

import com.swiftdrop.notification.event.DeliveryStatusUpdatedEvent;
import com.swiftdrop.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    @DisplayName("Should successfully delegate received DeliveryStatusUpdatedEvent to notification service")
    void shouldConsumeStatusUpdatedEvent() {
        DeliveryStatusUpdatedEvent event = DeliveryStatusUpdatedEvent.builder()
                .eventId("evt-test-99")
                .parcelId("PRC-EVT-01")
                .newStatus("DELIVERED")
                .timestamp(LocalDateTime.now())
                .build();

        listener.onDeliveryStatusUpdated(event);

        verify(notificationService).handleDeliveryStatusUpdated(event);
    }
}

