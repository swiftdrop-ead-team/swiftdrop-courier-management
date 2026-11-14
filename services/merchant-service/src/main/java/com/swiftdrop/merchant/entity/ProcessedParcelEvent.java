package com.swiftdrop.merchant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_processed_events")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ProcessedParcelEvent {

    @Id
    @Column(name = "event_key", length = 64)
    private String eventKey;

    @Column(name = "parcel_id", nullable = false, length = 100)
    private String parcelId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;
}
