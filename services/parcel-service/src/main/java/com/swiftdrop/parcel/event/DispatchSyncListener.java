package com.swiftdrop.parcel.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.swiftdrop.parcel.entity.ParcelStatusHistory;
import com.swiftdrop.parcel.enums.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.parcel.repository.ParcelStatusHistoryRepository;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;

/** Keeps the booking owner's parcel view synchronized with dispatch progress. */
@Component
public class DispatchSyncListener {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Event(String parcelId, String courierUserId, String status, String newStatus, String notes, String failureReason, LocalDateTime timestamp) {}
    private final ParcelRepository parcels;
    private final ParcelStatusHistoryRepository history;
    public DispatchSyncListener(ParcelRepository parcels, ParcelStatusHistoryRepository history) { this.parcels = parcels; this.history = history; }
    @RabbitListener(queues = "swiftdrop.parcel.dispatch-sync")
    @Transactional
    public void receive(Event event) {
        try {
            UUID id = UUID.fromString(event.parcelId());
            UUID courier = UUID.fromString(event.courierUserId());
            if (event.timestamp() == null) throw new IllegalArgumentException("Missing event timestamp");
            var parcel = parcels.findById(id).orElse(null);
            if (parcel == null) return;
            Instant timestamp = event.timestamp().atZone(ZoneId.of("Asia/Colombo")).toInstant();
            if (parcel.getDispatchUpdatedAt() != null && !timestamp.isAfter(parcel.getDispatchUpdatedAt())) return;
            if (parcel.getStatus() == ParcelStatus.DELIVERED || parcel.getStatus() == ParcelStatus.CANCELLED) return;
            String value = event.newStatus() != null ? event.newStatus() : event.status();
            ParcelStatus status = "ASSIGNED".equals(value) ? ParcelStatus.CREATED : ParcelStatus.valueOf(value);
            var previous = parcel.getStatus();
            parcel.setAssignedCourierId(courier); parcel.setDispatchUpdatedAt(timestamp); parcel.setStatus(status);
            if (status == ParcelStatus.FAILED) parcel.setFailureReason(event.failureReason());
            parcels.save(parcel);
            history.save(ParcelStatusHistory.builder().parcel(parcel).previousStatus(previous).newStatus(status).changedByUserId(courier).actorRole("ROLE_COURIER").notes(event.notes() == null ? "Dispatch update" : event.notes()).build());
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Invalid dispatch event", invalid);
        }
    }
    @Configuration
    public static class Bindings {
        @Bean Queue parcelDispatchSyncQueue() { return QueueBuilder.durable("swiftdrop.parcel.dispatch-sync").build(); }
        @Bean TopicExchange parcelDispatchExchange() { return new TopicExchange("dispatch.exchange", true, false); }
        @Bean Binding parcelAssignedBinding() { return BindingBuilder.bind(parcelDispatchSyncQueue()).to(parcelDispatchExchange()).with("dispatch.courier.assigned"); }
        @Bean Binding parcelStatusBinding() { return BindingBuilder.bind(parcelDispatchSyncQueue()).to(parcelDispatchExchange()).with("dispatch.status.updated"); }
    }
}
