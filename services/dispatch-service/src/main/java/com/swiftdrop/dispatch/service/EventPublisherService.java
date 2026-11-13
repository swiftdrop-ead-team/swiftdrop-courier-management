package com.swiftdrop.dispatch.service;

import com.swiftdrop.dispatch.event.CourierAssignedEvent;
import com.swiftdrop.dispatch.event.DeliveryStatusUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service dedicated to publishing asynchronous events to the RabbitMQ message broker.
 * Implements the Publisher/Subscriber integration pattern to decouple the Dispatch Service
 * from Notification Service, Tracking Service, and Merchant Webhooks.
 */
@Service
public class EventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);

    // RabbitTemplate injected by Spring AMQP for publishing messages
    private final RabbitTemplate rabbitTemplate;

    public EventPublisherService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Exchange name configured in application.yml
    @Value("${swiftdrop.dispatch.exchange:dispatch.exchange}")
    private String dispatchExchange;

    // Routing key for courier assigned events
    @Value("${swiftdrop.dispatch.routing-key-assigned:dispatch.courier.assigned}")
    private String courierAssignedRoutingKey;

    // Routing key for delivery status change events
    @Value("${swiftdrop.dispatch.routing-key-status-updated:dispatch.status.updated}")
    private String statusUpdatedRoutingKey;

    /**
     * Publishes a CourierAssignedEvent to RabbitMQ.
     * @param event The courier assignment event data payload.
     */
    public void publishCourierAssigned(CourierAssignedEvent event) {
        try {
            log.info("Publishing CourierAssignedEvent for parcel: {} to courier: {}", event.getParcelId(), event.getCourierId());
            rabbitTemplate.convertAndSend(dispatchExchange, courierAssignedRoutingKey, event);
        } catch (Exception ex) {
            // Logs the error gracefully so message broker issues do not abort local database transactions
            log.error("Failed to publish CourierAssignedEvent to RabbitMQ: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Publishes a DeliveryStatusUpdatedEvent to RabbitMQ.
     * @param event The status update event data payload.
     */
    public void publishDeliveryStatusUpdated(DeliveryStatusUpdatedEvent event) {
        try {
            log.info("Publishing DeliveryStatusUpdatedEvent for tracking: {} new status: {}", event.getTrackingNumber(), event.getNewStatus());
            rabbitTemplate.convertAndSend(dispatchExchange, statusUpdatedRoutingKey, event);
        } catch (Exception ex) {
            log.error("Failed to publish DeliveryStatusUpdatedEvent to RabbitMQ: {}", ex.getMessage(), ex);
        }
    }
}
