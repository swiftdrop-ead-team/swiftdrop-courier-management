package com.swiftdrop.tracking.messaging;
import com.swiftdrop.tracking.dto.DeliveryStatusUpdatedEvent;
import com.swiftdrop.tracking.service.TrackingService;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.Map;
@Component
public class ParcelEventListener {
 private final TrackingService service;
 public ParcelEventListener(TrackingService service) { this.service=service; }
 @RabbitListener(queues="swiftdrop.tracking.parcel-lifecycle")
 public void receive(Map<String,Object> payload) {
  var event=new DeliveryStatusUpdatedEvent();
  event.setEventId((String)payload.get("eventId"));event.setParcelId((String)payload.get("parcelId"));
  event.setTrackingNumber((String)payload.get("trackingNumber"));
  String type=(String)payload.get("eventType");
  event.setNewStatus("PARCEL_CANCELLED".equals(type)?"CANCELLED":(String)payload.getOrDefault("newStatus",payload.getOrDefault("status","CREATED")));
  event.setPreviousStatus((String)payload.get("previousStatus"));
  // Public history deliberately excludes sender/recipient contacts and cancellation free text.
  event.setNotes("PARCEL_CANCELLED".equals(type)?"Booking cancelled":"Parcel booking update");
  Object rawTimestamp = payload.get("timestamp");
  Instant timestamp;
  if (rawTimestamp instanceof Number number) {
   // Jackson AMQP emits Instant as epoch seconds with fractional nanoseconds.
   var epoch = new java.math.BigDecimal(number.toString());
   if (epoch.abs().compareTo(new java.math.BigDecimal("100000000000")) > 0) {
    timestamp = Instant.ofEpochMilli(number.longValue());
   } else {
    long seconds = epoch.longValue();
    long nanos = epoch.subtract(java.math.BigDecimal.valueOf(seconds)).movePointRight(9).longValue();
    timestamp = Instant.ofEpochSecond(seconds, nanos);
   }
  } else {
   timestamp = Instant.parse(rawTimestamp.toString());
  }
  event.setTimestamp(timestamp.atZone(ZoneId.of("Asia/Colombo")).toLocalDateTime());
  service.processEvent(event);
 }
 @Configuration public static class Bindings {
  @Bean Queue parcelLifecycleQueue(){return QueueBuilder.durable("swiftdrop.tracking.parcel-lifecycle").build();}
  @Bean TopicExchange parcelLifecycleExchange(){return new TopicExchange("swiftdrop.parcel.exchange",true,false);}
  @Bean Binding parcelLifecycleBinding(){return BindingBuilder.bind(parcelLifecycleQueue()).to(parcelLifecycleExchange()).with("parcel.#");}
 }
}
