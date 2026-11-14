package com.swiftdrop.parcel.event.payload;

import com.swiftdrop.parcel.enums.ParcelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelStatusUpdatedEvent {

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    @Builder.Default
    private String eventType = "PARCEL_STATUS_UPDATED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    private UUID parcelId;
    private String trackingNumber;
    private ParcelStatus previousStatus;
    private ParcelStatus newStatus;
    private UUID changedByUserId;
    private String actorRole;
    private String location;
    private String notes;
    private UUID assignedCourierId;
}

