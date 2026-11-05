package com.swiftdrop.tracking.repository;

import com.swiftdrop.tracking.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    boolean existsByEventId(String eventId);
    List<TrackingEvent> findByTrackingNumberOrderByOccurredAtAscVersionAsc(String trackingNumber);
}
