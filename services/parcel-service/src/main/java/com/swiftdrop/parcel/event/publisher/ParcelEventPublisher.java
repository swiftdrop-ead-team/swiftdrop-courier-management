package com.swiftdrop.parcel.event.publisher;

import com.swiftdrop.parcel.event.payload.ParcelCancelledEvent;
import com.swiftdrop.parcel.event.payload.ParcelCreatedEvent;
import com.swiftdrop.parcel.event.payload.ParcelStatusUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ParcelEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${swiftdrop.rabbitmq.exchange.parcel:swiftdrop.parcel.exchange}")
    private String parcelExchange;

    @Value("${swiftdrop.rabbitmq.routing-keys.parcel-created:parcel.created}")
    private String parcelCreatedRoutingKey;

    @Value("${swiftdrop.rabbitmq.routing-keys.parcel-status-updated:parcel.status_updated}")
    private String parcelStatusUpdatedRoutingKey;

    @Value("${swiftdrop.rabbitmq.routing-keys.parcel-cancelled:parcel.cancelled}")
    private String parcelCancelledRoutingKey;

    public ParcelEventPublisher(@Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishParcelCreated(ParcelCreatedEvent event) {
        publish(parcelCreatedRoutingKey, event);
    }

    public void publishParcelStatusUpdated(ParcelStatusUpdatedEvent event) {
        publish(parcelStatusUpdatedRoutingKey, event);
    }

    public void publishParcelCancelled(ParcelCancelledEvent event) {
        publish(parcelCancelledRoutingKey, event);
    }

    private void publish(String routingKey, Object payload) {
        if (rabbitTemplate == null) {
            log.info("RabbitMQ template not configured. Event skipped for routingKey {}: {}", routingKey, payload);
            return;
        }

        try {
            log.info("Publishing event to exchange [{}] with routing key [{}]: {}", parcelExchange, routingKey, payload);
            rabbitTemplate.convertAndSend(parcelExchange, routingKey, payload);
        } catch (Exception ex) {
            log.error("Failed to publish event to RabbitMQ for routingKey [{}]: {}. Business transaction proceeds.",
                    routingKey, ex.getMessage());
        }
    }
}

