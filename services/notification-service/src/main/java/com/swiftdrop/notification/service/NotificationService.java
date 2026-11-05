package com.swiftdrop.notification.service;

import com.swiftdrop.notification.entity.NotificationLog;
import com.swiftdrop.notification.entity.NotificationType;
import com.swiftdrop.notification.event.CourierAssignedEvent;
import com.swiftdrop.notification.event.DeliveryStatusUpdatedEvent;

import java.util.List;

/**
 * Service interface for handling incoming domain events and orchestrating notifications.
 */
public interface NotificationService {

    /**
     * Processes a delivery status update event and sends milestone emails.
     */
    void handleDeliveryStatusUpdated(DeliveryStatusUpdatedEvent event);

    /**
     * Processes a courier assignment event and alerts the courier.
     */
    void handleCourierAssigned(CourierAssignedEvent event);

    /**
     * Manually triggers a notification for testing / evaluator demonstration.
     */
    NotificationLog sendManualNotification(String parcelId, String trackingNumber,
                                          String recipientEmail, String recipientName,
                                          NotificationType type, String customMessage);

    /**
     * Retrieves all notification history logs for a parcel.
     */
    List<NotificationLog> getHistoryByParcelId(String parcelId);

    /**
     * Retrieves all notification logs in the system.
     */
    List<NotificationLog> getAllHistory();

    /**
     * Retrieves a single notification log by ID.
     */
    NotificationLog getNotificationById(Long id);
}
