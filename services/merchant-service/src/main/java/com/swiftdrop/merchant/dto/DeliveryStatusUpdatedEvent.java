package com.swiftdrop.merchant.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryStatusUpdatedEvent {

    private String eventId;
    private String eventType;
    private Long assignmentId;
    private String parcelId;
    private String trackingNumber;
    private Long courierId;
    private String previousStatus;
    private String newStatus;
    private String failureReason;
    private String notes;
    @JsonAlias("occurredAt")
    private LocalDateTime timestamp;
    private Long version;
}
