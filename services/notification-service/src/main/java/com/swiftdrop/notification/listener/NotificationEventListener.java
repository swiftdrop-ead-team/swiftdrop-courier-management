package com.swiftdrop.notification.listener;

import com.swiftdrop.notification.event.CourierAssignedEvent;
import com.swiftdrop.notification.event.DeliveryStatusUpdatedEvent;
import com.swiftdrop.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ asynchronous consumer for SwiftDrop domain events.
 * Listens to dispatch.status.updated and dispatch.courier.assigned.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    /**
     * Consumes status update events (e.g., OUT_FOR_DELIVERY, DELIVERED, FAILED).
     */
    @RabbitListener(queues = "${swiftdrop.notification.rabbitmq.queue-status-updated:swiftdrop.notification.status-updated.queue}")
    public void onDeliveryStatusUpdated(DeliveryStatusUpdatedEvent event) {
        log.info("Received DeliveryStatusUpdatedEvent for parcelId: {}, newStatus: {}",
                event.getParcelId(), event.getNewStatus());
        try {
            notificationService.handleDeliveryStatusUpdated(event);
        } catch (Exception ex) {
            log.error("Failed to process DeliveryStatusUpdatedEvent for parcelId: {}", event.getParcelId(), ex);
        }
    }

    /**
     * Consumes courier assignment events.
     */
    @RabbitListener(queues = "${swiftdrop.notification.rabbitmq.queue-courier-assigned:swiftdrop.notification.courier-assigned.queue}")
    public void onCourierAssigned(CourierAssignedEvent event) {
        log.info("Received CourierAssignedEvent for parcelId: {}, courier: {}",
                event.getParcelId(), event.getCourierName());
        try {
            notificationService.handleCourierAssigned(event);
        } catch (Exception ex) {
            log.error("Failed to process CourierAssignedEvent for parcelId: {}", event.getParcelId(), ex);
        }
    }
}

