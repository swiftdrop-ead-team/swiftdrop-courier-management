package com.swiftdrop.aiassistant.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ParcelStatusChangedEvent {
    private String eventId;
    private String parcelId;
    private String trackingNumber;
    private String previousStatus;
    private String newStatus;
    private LocalDateTime occurredAt;
    private Integer version;
}