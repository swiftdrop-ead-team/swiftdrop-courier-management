package com.swiftdrop.parcel.dto.response;

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
public class ParcelStatusHistoryResponse {

    private UUID id;
    private ParcelStatus previousStatus;
    private ParcelStatus newStatus;
    private UUID changedByUserId;
    private String actorRole;
    private String location;
    private String notes;
    private Instant timestamp;
}

