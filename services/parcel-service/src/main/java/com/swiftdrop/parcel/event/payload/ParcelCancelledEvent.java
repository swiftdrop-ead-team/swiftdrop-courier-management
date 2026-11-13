package com.swiftdrop.parcel.event.payload;

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
public class ParcelCancelledEvent {

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    @Builder.Default
    private String eventType = "PARCEL_CANCELLED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    private UUID parcelId;
    private String trackingNumber;
    private UUID cancelledByUserId;
    private String actorRole;
    private String reason;
}

