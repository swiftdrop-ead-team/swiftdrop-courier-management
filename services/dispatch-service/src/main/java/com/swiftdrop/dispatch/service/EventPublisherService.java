package com.swiftdrop.dispatch.service;

import com.swiftdrop.dispatch.event.CourierAssignedEvent;
import com.swiftdrop.dispatch.event.DeliveryStatusUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${swiftdrop.dispatch.exchange:dispatch.exchange}")
    private String dispatchExchange;

    @Value("${swiftdrop.dispatch.routing-key-assigned:dispatch.courier.assigned}")
    private String courierAssignedRoutingKey;

    @Value("${swiftdrop.dispatch.routing-key-status-updated:dispatch.status.updated}")
    private String statusUpdatedRoutingKey;

    public void publishCourierAssigned(CourierAssignedEvent event) {
        try {
            log.info("Publishing CourierAssignedEvent for parcel: {} to courier: {}", event.getParcelId(), event.getCourierId());
            rabbitTemplate.convertAndSend(dispatchExchange, courierAssignedRoutingKey, event);
        } catch (Exception ex) {
            log.error("Failed to publish CourierAssignedEvent to RabbitMQ: {}", ex.getMessage(), ex);
        }
    }

    public void publishDeliveryStatusUpdated(DeliveryStatusUpdatedEvent event) {
        try {
            log.info("Publishing DeliveryStatusUpdatedEvent for tracking: {} new status: {}", event.getTrackingNumber(), event.getNewStatus());
            rabbitTemplate.convertAndSend(dispatchExchange, statusUpdatedRoutingKey, event);
        } catch (Exception ex) {
            log.error("Failed to publish DeliveryStatusUpdatedEvent to RabbitMQ: {}", ex.getMessage(), ex);
        }
    }
}
