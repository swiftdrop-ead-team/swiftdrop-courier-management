package com.swiftdrop.tracking.service;

import com.swiftdrop.tracking.dto.DeliveryStatusUpdatedEvent;
import com.swiftdrop.tracking.entity.TrackingParcel;
import com.swiftdrop.tracking.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class TrackingServiceTest {
    @Mock TrackingParcelRepository parcelRepository;
    @Mock TrackingEventRepository eventRepository;
    @Mock TrackingStreamService streamService;

    @Test
    void storesEventAndSetsCurrentStatus() {
        TrackingService service = new TrackingService(parcelRepository, eventRepository, streamService);
        when(eventRepository.existsByEventId("evt-1")).thenReturn(false);
        when(parcelRepository.findByTrackingNumberForUpdate("SD-1234")).thenReturn(Optional.empty());

        DeliveryStatusUpdatedEvent event = event("evt-1", "IN_TRANSIT", 2L,
                LocalDateTime.of(2026, 11, 5, 10, 30));
        service.processEvent(event);

        ArgumentCaptor<TrackingParcel> parcel = ArgumentCaptor.forClass(TrackingParcel.class);
        verify(parcelRepository).save(parcel.capture());
        org.junit.jupiter.api.Assertions.assertEquals("IN_TRANSIT", parcel.getValue().getCurrentStatus());
        verify(eventRepository).save(any());
        verify(streamService).broadcast(eq("SD-1234"), any());
    }

    @Test
    void duplicateEventIsIgnored() {
        TrackingService service = new TrackingService(parcelRepository, eventRepository, streamService);
        when(eventRepository.existsByEventId("evt-1")).thenReturn(true);

        service.processEvent(event("evt-1", "IN_TRANSIT", 2L, LocalDateTime.now()));

        verifyNoInteractions(parcelRepository, streamService);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void olderVersionDoesNotOverwriteCurrentStatus() {
        TrackingParcel parcel = new TrackingParcel();
        parcel.setId(1L);
        parcel.setParcelId("parcel-1");
        parcel.setTrackingNumber("SD-1234");
        parcel.setCurrentStatus("DELIVERED");
        parcel.setLastEventVersion(3L);
        parcel.setLastUpdatedAt(LocalDateTime.of(2026, 11, 5, 12, 0));

        TrackingService service = new TrackingService(parcelRepository, eventRepository, streamService);
        when(eventRepository.existsByEventId("evt-old")).thenReturn(false);
        when(parcelRepository.findByTrackingNumberForUpdate("SD-1234")).thenReturn(Optional.of(parcel));

        service.processEvent(event("evt-old", "IN_TRANSIT", 2L,
                LocalDateTime.of(2026, 11, 5, 11, 0)));

        org.junit.jupiter.api.Assertions.assertEquals("DELIVERED", parcel.getCurrentStatus());
        verify(eventRepository).save(any());
        verify(parcelRepository, never()).save(any());
    }

    private DeliveryStatusUpdatedEvent event(String id, String status, Long version, LocalDateTime timestamp) {
        DeliveryStatusUpdatedEvent event = new DeliveryStatusUpdatedEvent();
        event.setEventId(id);
        event.setParcelId("parcel-1");
        event.setTrackingNumber("SD-1234");
        event.setPreviousStatus("ASSIGNED");
        event.setNewStatus(status);
        event.setVersion(version);
        event.setTimestamp(timestamp);
        return event;
    }
}
