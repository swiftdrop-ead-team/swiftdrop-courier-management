package com.swiftdrop.notification.service;

import com.swiftdrop.notification.entity.NotificationChannel;
import com.swiftdrop.notification.entity.NotificationLog;
import com.swiftdrop.notification.entity.NotificationStatus;
import com.swiftdrop.notification.entity.NotificationType;
import com.swiftdrop.notification.event.CourierAssignedEvent;
import com.swiftdrop.notification.event.DeliveryStatusUpdatedEvent;
import com.swiftdrop.notification.exception.ResourceNotFoundException;
import com.swiftdrop.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core business service implementing notification orchestration, idempotency checks,
 * HTML email generation, and delivery audit logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final EmailSenderService emailSenderService;
    private final EmailTemplateService emailTemplateService;

    @Override
    @Transactional
    public void handleDeliveryStatusUpdated(DeliveryStatusUpdatedEvent event) {
        String parcelId = event.getParcelId();
        String newStatus = event.getNewStatus();

        if (parcelId == null || newStatus == null) {
            log.warn("Skipping DeliveryStatusUpdatedEvent: missing parcelId or newStatus");
            return;
        }

        // 1. Idempotency Check Layer 1: Check by Event ID
        if (event.getEventId() != null && notificationLogRepository.existsByEventId(event.getEventId())) {
            log.info("Duplicate event ignored for eventId: {}", event.getEventId());
            return;
        }

        // 2. Map status to customer milestone.
        // Intermediate internal statuses (IN_TRANSIT, PICKED_UP) are filtered out to avoid email spam.
        NotificationType milestoneType;
        String subject;
        String htmlBody;

        String recipientEmail = (event.getRecipientEmail() != null && !event.getRecipientEmail().isBlank())
                ? event.getRecipientEmail()
                : "customer@swiftdrop.io";
        String recipientName = (event.getRecipientName() != null && !event.getRecipientName().isBlank())
                ? event.getRecipientName()
                : "Valued Customer";
        String trackingNumber = (event.getTrackingNumber() != null) ? event.getTrackingNumber() : parcelId;

        switch (newStatus.toUpperCase()) {
            case "OUT_FOR_DELIVERY":
                milestoneType = NotificationType.OUT_FOR_DELIVERY;
                subject = "SwiftDrop: Your parcel #" + trackingNumber + " is out for delivery!";
                htmlBody = emailTemplateService.buildOutForDeliveryEmail(
                        recipientName, parcelId, trackingNumber, event.getCourierName());
                break;

            case "DELIVERED":
                milestoneType = NotificationType.DELIVERED;
                subject = "SwiftDrop: Your parcel #" + trackingNumber + " has been delivered!";
                htmlBody = emailTemplateService.buildDeliveredEmail(
                        recipientName, parcelId, trackingNumber);
                break;

            case "FAILED":
                milestoneType = NotificationType.DELIVERY_FAILED;
                subject = "SwiftDrop: Delivery attempt failed for parcel #" + trackingNumber;
                htmlBody = emailTemplateService.buildDeliveryFailedEmail(
                        recipientName, parcelId, trackingNumber, event.getFailureReason());
                break;

            default:
                // Skip internal logistical scans (PICKED_UP, IN_TRANSIT, etc.)
                log.info("Status '{}' does not require a customer email notification. Skipping.", newStatus);
                return;
        }

        // 3. Idempotency Check Layer 2: Check by Parcel ID + Milestone
        // Guarantees the customer never receives the same milestone email twice.
        if (notificationLogRepository.existsByParcelIdAndNotificationTypeAndDeliveryStatus(
                parcelId, milestoneType, NotificationStatus.SENT)) {
            log.info("Milestone '{}' email was already sent for parcelId: {}. Skipping duplicate dispatch.",
                    milestoneType, parcelId);
            return;
        }

        // 4. Dispatch Email
        boolean sentSuccess = emailSenderService.sendHtmlEmail(recipientEmail, subject, htmlBody);

        // 5. Save audit log to swiftdrop_notification database
        NotificationLog logEntry = NotificationLog.builder()
                .eventId(event.getEventId())
                .parcelId(parcelId)
                .trackingNumber(trackingNumber)
                .recipientEmail(recipientEmail)
                .recipientName(recipientName)
                .notificationType(milestoneType)
                .channel(NotificationChannel.EMAIL)
                .subject(subject)
                .messageBody(htmlBody)
                .deliveryStatus(sentSuccess ? NotificationStatus.SENT : NotificationStatus.FAILED)
                .retryCount(0)
                .errorMessage(sentSuccess ? null : "Email delivery failed")
                .sentAt(sentSuccess ? LocalDateTime.now() : null)
                .build();

        notificationLogRepository.save(logEntry);
        log.info("Successfully recorded notification log for parcelId: {}, milestone: {}, status: {}",
                parcelId, milestoneType, logEntry.getDeliveryStatus());
    }

    @Override
    @Transactional
    public void handleCourierAssigned(CourierAssignedEvent event) {
        String parcelId = event.getParcelId();

        if (parcelId == null) {
            log.warn("Skipping CourierAssignedEvent: missing parcelId");
            return;
        }

        // Idempotency Check Layer 1
        if (event.getEventId() != null && notificationLogRepository.existsByEventId(event.getEventId())) {
            log.info("Duplicate CourierAssignedEvent ignored for eventId: {}", event.getEventId());
            return;
        }

        // Idempotency Check Layer 2
        if (notificationLogRepository.existsByParcelIdAndNotificationTypeAndDeliveryStatus(
                parcelId, NotificationType.COURIER_ASSIGNED, NotificationStatus.SENT)) {
            log.info("COURIER_ASSIGNED notification already sent for parcelId: {}. Skipping duplicate.", parcelId);
            return;
        }

        String courierEmail = (event.getCourierEmail() != null && !event.getCourierEmail().isBlank())
                ? event.getCourierEmail()
                : "courier@swiftdrop.io";
        String courierName = (event.getCourierName() != null) ? event.getCourierName() : "Field Courier";
        String trackingNumber = (event.getTrackingNumber() != null) ? event.getTrackingNumber() : parcelId;
        String subject = "SwiftDrop: New Parcel Assignment #" + trackingNumber;

        String htmlBody = emailTemplateService.buildCourierAssignedEmail(
                courierName, parcelId, trackingNumber,
                event.getPickupAddress(), event.getDeliveryAddress(),
                event.getRecipientName(), event.getRecipientPhone());

        boolean sentSuccess = emailSenderService.sendHtmlEmail(courierEmail, subject, htmlBody);

        NotificationLog logEntry = NotificationLog.builder()
                .eventId(event.getEventId())
                .parcelId(parcelId)
                .trackingNumber(trackingNumber)
                .recipientEmail(courierEmail)
                .recipientName(courierName)
                .notificationType(NotificationType.COURIER_ASSIGNED)
                .channel(NotificationChannel.EMAIL)
                .subject(subject)
                .messageBody(htmlBody)
                .deliveryStatus(sentSuccess ? NotificationStatus.SENT : NotificationStatus.FAILED)
                .retryCount(0)
                .sentAt(sentSuccess ? LocalDateTime.now() : null)
                .build();

        notificationLogRepository.save(logEntry);
        log.info("Recorded courier assignment notification log for parcelId: {}, courier: {}",
                parcelId, courierName);
    }

    @Override
    @Transactional
    public NotificationLog sendManualNotification(String parcelId, String trackingNumber,
                                                 String recipientEmail, String recipientName,
                                                 NotificationType type, String customMessage) {
        String safeTracking = (trackingNumber != null) ? trackingNumber : parcelId;
        String safeRecipient = (recipientEmail != null && !recipientEmail.isBlank())
                ? recipientEmail
                : "customer@swiftdrop.io";
        String safeName = (recipientName != null && !recipientName.isBlank()) ? recipientName : "Valued Customer";
        String subject = "SwiftDrop Notification: " + type.name().replace('_', ' ') + " - #" + safeTracking;

        String htmlBody;
        switch (type) {
            case ORDER_CONFIRMED:
                htmlBody = emailTemplateService.buildOrderConfirmedEmail(safeName, parcelId, safeTracking);
                break;
            case OUT_FOR_DELIVERY:
                htmlBody = emailTemplateService.buildOutForDeliveryEmail(safeName, parcelId, safeTracking, "SwiftDrop Courier");
                break;
            case DELIVERED:
                htmlBody = emailTemplateService.buildDeliveredEmail(safeName, parcelId, safeTracking);
                break;
            case DELIVERY_FAILED:
                htmlBody = emailTemplateService.buildDeliveryFailedEmail(safeName, parcelId, safeTracking, customMessage);
                break;
            default:
                htmlBody = "<p>" + (customMessage != null ? customMessage : "SwiftDrop notification update.") + "</p>";
        }

        boolean sentSuccess = emailSenderService.sendHtmlEmail(safeRecipient, subject, htmlBody);

        NotificationLog logEntry = NotificationLog.builder()
                .parcelId(parcelId)
                .trackingNumber(safeTracking)
                .recipientEmail(safeRecipient)
                .recipientName(safeName)
                .notificationType(type)
                .channel(NotificationChannel.EMAIL)
                .subject(subject)
                .messageBody(htmlBody)
                .deliveryStatus(sentSuccess ? NotificationStatus.SENT : NotificationStatus.FAILED)
                .retryCount(0)
                .sentAt(sentSuccess ? LocalDateTime.now() : null)
                .build();

        return notificationLogRepository.save(logEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLog> getHistoryByParcelId(String parcelId) {
        return notificationLogRepository.findByParcelIdOrderByCreatedAtDesc(parcelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLog> getAllHistory() {
        return notificationLogRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationLog getNotificationById(Long id) {
        return notificationLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification record not found with ID: " + id));
    }
}

