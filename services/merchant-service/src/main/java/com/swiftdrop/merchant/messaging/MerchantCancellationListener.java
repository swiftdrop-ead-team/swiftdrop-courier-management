package com.swiftdrop.merchant.messaging;
import com.swiftdrop.merchant.dto.DeliveryStatusUpdatedEvent;
import com.swiftdrop.merchant.service.MerchantParcelEventService;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.Map;
@Component
public class MerchantCancellationListener {
 private final MerchantParcelEventService events;
 public MerchantCancellationListener(MerchantParcelEventService events){this.events=events;}
 @RabbitListener(queues="swiftdrop.merchant.parcel-cancelled",autoStartup="${spring.rabbitmq.listener.simple.auto-startup:true}")
 public void receive(Map<String,Object> payload){
  Object raw=payload.get("timestamp"); Instant instant;
  if(raw instanceof Number n){var epoch=new java.math.BigDecimal(n.toString());long seconds=epoch.longValue();instant=Instant.ofEpochSecond(seconds,epoch.subtract(java.math.BigDecimal.valueOf(seconds)).movePointRight(9).longValue());}
  else instant=Instant.parse(raw.toString());
  events.process(DeliveryStatusUpdatedEvent.builder().eventId((String)payload.get("eventId"))
   .parcelId((String)payload.get("parcelId")).trackingNumber((String)payload.get("trackingNumber"))
   .newStatus("CANCELLED").eventType("PARCEL_CANCELLED").timestamp(instant.atZone(ZoneId.of("Asia/Colombo")).toLocalDateTime()).build(),null);
 }
 @Configuration public static class Bindings {
  @Bean Queue merchantCancellationQueue(){return QueueBuilder.durable("swiftdrop.merchant.parcel-cancelled").build();}
  @Bean TopicExchange merchantParcelExchange(){return new TopicExchange("swiftdrop.parcel.exchange",true,false);}
  @Bean Binding merchantCancellationBinding(){return BindingBuilder.bind(merchantCancellationQueue()).to(merchantParcelExchange()).with("parcel.cancelled");}
 }
}
