package com.swiftdrop.tracking.messaging;

import com.swiftdrop.tracking.dto.DeliveryStatusUpdatedEvent;
import com.swiftdrop.tracking.service.TrackingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TrackingEventListener {
    private final TrackingService trackingService;

    public TrackingEventListener(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @RabbitListener(queues = "${swiftdrop.tracking.queue}")
    public void onStatusUpdated(DeliveryStatusUpdatedEvent event) {
        trackingService.processEvent(event);
    }
}
