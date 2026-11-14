package com.swiftdrop.tracking.service;
import com.swiftdrop.tracking.messaging.ParcelEventListener;
import com.swiftdrop.tracking.dto.DeliveryStatusUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ParcelEventListenerTest {
 @Test void acceptsNumericTimestampProducedByRabbitConverter() {
  var service=mock(TrackingService.class);var listener=new ParcelEventListener(service);
  listener.receive(Map.of("eventType","PARCEL_CREATED","eventId","numeric","parcelId","p1","trackingNumber","SD-1","status","CREATED","timestamp",1794633655.8490248));
  var event=ArgumentCaptor.forClass(DeliveryStatusUpdatedEvent.class);verify(service).processEvent(event.capture());
  assertEquals(java.time.Instant.ofEpochSecond(1794633655L,849024800).atZone(java.time.ZoneId.of("Asia/Colombo")).toLocalDateTime(),event.getValue().getTimestamp());
 }
 @Test void convertsBookingTimestampToServiceTimezone() {
  var service=mock(TrackingService.class);var listener=new ParcelEventListener(service);
  listener.receive(Map.of("eventType","PARCEL_CREATED","eventId","e1","parcelId","p1","trackingNumber","SD-1","status","CREATED","timestamp","2026-11-14T04:00:00Z"));
  var event=ArgumentCaptor.forClass(DeliveryStatusUpdatedEvent.class);verify(service).processEvent(event.capture());
  assertEquals("CREATED",event.getValue().getNewStatus());assertEquals(9,event.getValue().getTimestamp().getHour());
  assertEquals(30,event.getValue().getTimestamp().getMinute());
 }
 @Test void cancellationExcludesPrivateReason() {
  var service=mock(TrackingService.class);var listener=new ParcelEventListener(service);
  listener.receive(Map.of("eventType","PARCEL_CANCELLED","eventId","e2","parcelId","p1","trackingNumber","SD-1","reason","private contact details","timestamp","2026-11-14T04:00:00Z"));
  var event=ArgumentCaptor.forClass(DeliveryStatusUpdatedEvent.class);verify(service).processEvent(event.capture());
  assertEquals("CANCELLED",event.getValue().getNewStatus());assertEquals("Booking cancelled",event.getValue().getNotes());
 }
}
