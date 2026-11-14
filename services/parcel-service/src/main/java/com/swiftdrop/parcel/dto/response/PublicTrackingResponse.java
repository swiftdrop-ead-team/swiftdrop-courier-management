package com.swiftdrop.parcel.dto.response;

import com.swiftdrop.parcel.enums.DeliveryType;
import com.swiftdrop.parcel.enums.ParcelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicTrackingResponse {

    private String trackingNumber;
    private ParcelStatus status;
    private DeliveryType deliveryType;
    private String originCity;
    private String destinationCity;
    private boolean isFragile;
    private Instant createdAt;
    private Instant updatedAt;
    private List<PublicStatusCheckpoint> checkpoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicStatusCheckpoint {
        private ParcelStatus status;
        private String location;
        private String notes;
        private Instant timestamp;
    }
}

