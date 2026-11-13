package com.swiftdrop.tracking.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TrackingResponse(String trackingNumber, String currentStatus, String statusDescription,
                               LocalDateTime lastUpdatedAt, List<TrackingEventResponse> timeline) {
}
