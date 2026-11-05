package com.swiftdrop.tracking.service;

import com.swiftdrop.tracking.dto.*;
import com.swiftdrop.tracking.entity.*;
import com.swiftdrop.tracking.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HexFormat;

@Service
public class TrackingService {
    private final TrackingParcelRepository parcelRepository;
    private final TrackingEventRepository eventRepository;
    private final TrackingStreamService streamService;

    public TrackingService(TrackingParcelRepository parcelRepository, TrackingEventRepository eventRepository,
                           TrackingStreamService streamService) {
        this.parcelRepository = parcelRepository;
        this.eventRepository = eventRepository;
        this.streamService = streamService;
    }

    @Transactional
    public void processEvent(DeliveryStatusUpdatedEvent event) {
        validate(event);
        String eventId = eventId(event);
        if (eventRepository.existsByEventId(eventId)) {
            return;
        }

        LocalDateTime occurredAt = event.getTimestamp() == null ? LocalDateTime.now() : event.getTimestamp();
        TrackingParcel parcel = parcelRepository.findByTrackingNumberForUpdate(event.getTrackingNumber())
                .orElseGet(() -> {
                    TrackingParcel created = new TrackingParcel();
                    created.setParcelId(event.getParcelId());
                    created.setTrackingNumber(event.getTrackingNumber());
                    created.setCurrentStatus(event.getNewStatus());
                    created.setLastEventVersion(event.getVersion());
                    created.setLastUpdatedAt(occurredAt);
                    return created;
                });
        if (parcel.getId() != null && !event.getParcelId().equals(parcel.getParcelId())) {
            throw new IllegalArgumentException("Tracking number is already associated with another parcel");
        }

        TrackingEvent history = new TrackingEvent();
        history.setEventId(eventId);
        history.setParcelId(event.getParcelId());
        history.setTrackingNumber(event.getTrackingNumber());
        history.setPreviousStatus(event.getPreviousStatus());
        history.setNewStatus(event.getNewStatus());
        history.setDescription(event.getFailureReason() != null ? event.getFailureReason() : event.getNotes());
        history.setOccurredAt(occurredAt);
        history.setVersion(event.getVersion());
        eventRepository.save(history);

        if (isNewer(event.getVersion(), occurredAt, parcel)) {
            parcel.setCurrentStatus(event.getNewStatus());
            parcel.setLastEventVersion(event.getVersion());
            parcel.setLastUpdatedAt(occurredAt);
            parcelRepository.save(parcel);
        } else if (parcel.getId() == null) {
            parcelRepository.save(parcel);
        }
        streamService.broadcast(event.getTrackingNumber(), toResponse(parcel));
    }

    @Transactional(readOnly = true)
    public TrackingResponse getTracking(String trackingNumber) {
        TrackingParcel parcel = parcelRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new TrackingNotFoundException(trackingNumber));
        return toResponse(parcel);
    }

    private TrackingResponse toResponse(TrackingParcel parcel) {
        List<TrackingEventResponse> timeline = eventRepository
                .findByTrackingNumberOrderByOccurredAtAscVersionAsc(parcel.getTrackingNumber())
                .stream()
                .map(event -> new TrackingEventResponse(event.getPreviousStatus(), event.getNewStatus(),
                        event.getDescription(), event.getOccurredAt()))
                .toList();
        return new TrackingResponse(parcel.getTrackingNumber(), parcel.getCurrentStatus(),
                description(parcel.getCurrentStatus()), parcel.getLastUpdatedAt(), timeline);
    }

    private boolean isNewer(Long version, LocalDateTime occurredAt, TrackingParcel parcel) {
        if (version != null && parcel.getLastEventVersion() != null) {
            return version > parcel.getLastEventVersion();
        }
        if (version != null) {
            return true;
        }
        return parcel.getLastEventVersion() == null && occurredAt.isAfter(parcel.getLastUpdatedAt());
    }

    private void validate(DeliveryStatusUpdatedEvent event) {
        if (event.getParcelId() == null || event.getParcelId().isBlank()
                || event.getTrackingNumber() == null || event.getTrackingNumber().isBlank()
                || event.getNewStatus() == null || event.getNewStatus().isBlank()) {
            throw new IllegalArgumentException("Status event requires parcelId, trackingNumber and newStatus");
        }
    }

    private String eventId(DeliveryStatusUpdatedEvent event) {
        if (event.getEventId() != null && !event.getEventId().isBlank()) {
            return event.getEventId();
        }
        String source = String.join("|", nullToEmpty(event.getParcelId()), nullToEmpty(event.getTrackingNumber()),
                nullToEmpty(event.getPreviousStatus()), nullToEmpty(event.getNewStatus()),
                String.valueOf(event.getTimestamp()), String.valueOf(event.getVersion()));
        try {
            return "derived-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for event idempotency", exception);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String description(String status) {
        return switch (status) {
            case "ASSIGNED" -> "A courier has been assigned";
            case "PICKED_UP" -> "The parcel has been picked up";
            case "IN_TRANSIT" -> "The parcel is in transit";
            case "OUT_FOR_DELIVERY" -> "The parcel is out for delivery";
            case "DELIVERED" -> "The parcel was delivered";
            case "FAILED" -> "Delivery could not be completed";
            case "CANCELLED" -> "The parcel delivery was cancelled";
            default -> "The parcel status was updated";
        };
    }
}
