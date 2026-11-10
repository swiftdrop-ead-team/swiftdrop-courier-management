package com.swiftdrop.tracking.controller;

import com.swiftdrop.tracking.dto.TrackingResponse;
import com.swiftdrop.tracking.service.*;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/tracking")
@Validated
public class TrackingController {
    private final TrackingService trackingService;
    private final TrackingStreamService streamService;

    public TrackingController(TrackingService trackingService, TrackingStreamService streamService) {
        this.trackingService = trackingService;
        this.streamService = streamService;
    }

    @GetMapping("/{trackingNumber}")
    public TrackingResponse getPublicTracking(@PathVariable @Pattern(regexp = "^[A-Za-z0-9-]{4,50}$") String trackingNumber) {
        return trackingService.getTracking(trackingNumber);
    }

    @GetMapping("/{trackingNumber}/history")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_MERCHANT','ROLE_ADMIN','ROLE_COURIER')")
    public TrackingResponse getHistory(@PathVariable @Pattern(regexp = "^[A-Za-z0-9-]{4,50}$") String trackingNumber) {
        return trackingService.getTracking(trackingNumber);
    }

    @GetMapping(value = "/{trackingNumber}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable @Pattern(regexp = "^[A-Za-z0-9-]{4,50}$") String trackingNumber) {
        TrackingResponse snapshot = trackingService.getTracking(trackingNumber);
        return streamService.subscribe(trackingNumber, snapshot);
    }
}
