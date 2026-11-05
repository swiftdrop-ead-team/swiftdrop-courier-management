package com.swiftdrop.notification.repository;

import com.swiftdrop.notification.entity.NotificationLog;
import com.swiftdrop.notification.entity.NotificationStatus;
import com.swiftdrop.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for notification history queries and duplicate checks.
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    /**
     * Checks if an event has already been processed using its unique Event ID (idempotency check).
     */
    boolean existsByEventId(String eventId);

    /**
     * Checks if a milestone notification was already successfully sent for this parcel and status.
     * Prevents duplicate milestone emails to the customer.
     */
    boolean existsByParcelIdAndNotificationTypeAndDeliveryStatus(
            String parcelId,
            NotificationType notificationType,
            NotificationStatus deliveryStatus
    );

    /**
     * Retrieves all notification history logs for a specific parcel.
     */
    List<NotificationLog> findByParcelIdOrderByCreatedAtDesc(String parcelId);

    /**
     * Retrieves all notification logs for a specific recipient email.
     */
    List<NotificationLog> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);
}

