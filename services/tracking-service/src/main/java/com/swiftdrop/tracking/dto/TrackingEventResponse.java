package com.swiftdrop.tracking.dto;

import java.time.LocalDateTime;

public record TrackingEventResponse(String previousStatus, String newStatus, String description,
                                    LocalDateTime occurredAt) {
}
