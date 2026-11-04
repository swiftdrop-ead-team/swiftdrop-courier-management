package com.swiftdrop.dispatch.service;

import com.swiftdrop.dispatch.dto.*;
import com.swiftdrop.dispatch.entity.*;
import com.swiftdrop.dispatch.event.CourierAssignedEvent;
import com.swiftdrop.dispatch.event.DeliveryStatusUpdatedEvent;
import com.swiftdrop.dispatch.exception.CourierOverloadException;
import com.swiftdrop.dispatch.exception.CourierUnavailableException;
import com.swiftdrop.dispatch.exception.InvalidAssignmentException;
import com.swiftdrop.dispatch.exception.ResourceNotFoundException;
import com.swiftdrop.dispatch.repository.CourierRepository;
import com.swiftdrop.dispatch.repository.DispatchAssignmentRepository;
import com.swiftdrop.dispatch.repository.ProofOfDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DispatchAssignmentServiceImpl implements DispatchAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(DispatchAssignmentServiceImpl.class);

    private final DispatchAssignmentRepository assignmentRepository;
    private final CourierRepository courierRepository;
    private final ProofOfDeliveryRepository proofOfDeliveryRepository;
    private final EventPublisherService eventPublisherService;

    private static final List<AssignmentStatus> ACTIVE_STATUSES = List.of(
            AssignmentStatus.ASSIGNED,
            AssignmentStatus.PICKED_UP,
            AssignmentStatus.IN_TRANSIT,
            AssignmentStatus.OUT_FOR_DELIVERY
    );

    @Override
    @Transactional
    public DispatchAssignmentResponse assignCourier(AssignCourierRequest request) {
        log.info("Assigning parcel: {} to courierId: {}", request.getParcelId(), request.getCourierId());

        Courier courier = courierRepository.findById(request.getCourierId())
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found with id: " + request.getCourierId()));

        if (courier.getStatus() == CourierStatus.OFFLINE) {
            throw new CourierUnavailableException("Courier '" + courier.getFullName() + "' is currently OFFLINE");
        }

        if (!courier.canAcceptMoreDeliveries()) {
            throw new CourierOverloadException(String.format(
                    "Courier '%s' has reached maximum active delivery limit (%d/%d)",
                    courier.getFullName(), courier.getCurrentActiveDeliveries(), courier.getMaxActiveDeliveries()
            ));
        }

        // Check if parcel is already actively assigned
        Optional<DispatchAssignment> existingAssignment = assignmentRepository.findByParcelId(request.getParcelId());
        if (existingAssignment.isPresent() && ACTIVE_STATUSES.contains(existingAssignment.get().getStatus())) {
            throw new InvalidAssignmentException("Parcel '" + request.getParcelId() + "' is already assigned to a courier with status: "
                    + existingAssignment.get().getStatus());
        }

        DispatchAssignment assignment = DispatchAssignment.builder()
                .parcelId(request.getParcelId())
                .courier(courier)
                .trackingNumber(request.getTrackingNumber())
                .pickupAddress(request.getPickupAddress())
                .deliveryAddress(request.getDeliveryAddress())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .status(AssignmentStatus.ASSIGNED)
                .notes(request.getNotes())
                .assignedAt(LocalDateTime.now())
                .build();

        courier.incrementDeliveries();
        courierRepository.save(courier);
        DispatchAssignment savedAssignment = assignmentRepository.save(assignment);

        // Publish event to RabbitMQ
        CourierAssignedEvent event = CourierAssignedEvent.builder()
                .assignmentId(savedAssignment.getId())
                .parcelId(savedAssignment.getParcelId())
                .courierId(courier.getId())
                .courierName(courier.getFullName())
                .courierPhone(courier.getPhoneNumber())
                .trackingNumber(savedAssignment.getTrackingNumber())
                .pickupAddress(savedAssignment.getPickupAddress())
                .deliveryAddress(savedAssignment.getDeliveryAddress())
                .recipientName(savedAssignment.getRecipientName())
                .recipientPhone(savedAssignment.getRecipientPhone())
                .status(savedAssignment.getStatus().name())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisherService.publishCourierAssigned(event);

        return mapToResponse(savedAssignment);
    }

    @Override
    @Transactional
    public DispatchAssignmentResponse updateAssignmentStatus(Long assignmentId, UpdateAssignmentStatusRequest request) {
        log.info("Updating status for assignmentId: {} to {}", assignmentId, request.getStatus());

        DispatchAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        AssignmentStatus previousStatus = assignment.getStatus();
        AssignmentStatus newStatus = request.getStatus();

        assignment.setStatus(newStatus);
        if (request.getNotes() != null) {
            assignment.setNotes(request.getNotes());
        }

        if (newStatus == AssignmentStatus.FAILED) {
            if (request.getFailureReason() == null || request.getFailureReason().isBlank()) {
                throw new InvalidAssignmentException("Failure reason is mandatory when marking delivery as FAILED");
            }
            assignment.setFailureReason(request.getFailureReason());
        }

        // If completed or failed, decrement courier deliveries
        boolean wasActive = ACTIVE_STATUSES.contains(previousStatus);
        boolean isNowTerminal = !ACTIVE_STATUSES.contains(newStatus);

        if (wasActive && isNowTerminal) {
            assignment.setCompletedAt(LocalDateTime.now());
            Courier courier = assignment.getCourier();
            if (courier != null) {
                courier.decrementDeliveries();
                courierRepository.save(courier);
            }
        }

        DispatchAssignment updated = assignmentRepository.save(assignment);

        // Publish event to RabbitMQ
        DeliveryStatusUpdatedEvent event = DeliveryStatusUpdatedEvent.builder()
                .assignmentId(updated.getId())
                .parcelId(updated.getParcelId())
                .trackingNumber(updated.getTrackingNumber())
                .courierId(updated.getCourier().getId())
                .previousStatus(previousStatus.name())
                .newStatus(newStatus.name())
                .failureReason(updated.getFailureReason())
                .notes(updated.getNotes())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisherService.publishDeliveryStatusUpdated(event);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ProofOfDeliveryResponse submitProofOfDelivery(Long assignmentId, ProofOfDeliveryRequest request) {
        log.info("Submitting proof of delivery for assignmentId: {}", assignmentId);

        DispatchAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        if (assignment.getProofOfDelivery() != null) {
            throw new InvalidAssignmentException("Proof of delivery already recorded for assignment: " + assignmentId);
        }

        ProofOfDelivery pod = ProofOfDelivery.builder()
                .assignment(assignment)
                .recipientName(request.getRecipientName())
                .recipientNic(request.getRecipientNic())
                .signatureUrl(request.getSignatureUrl())
                .photoUrl(request.getPhotoUrl())
                .deliveryNotes(request.getDeliveryNotes())
                .deliveredAt(LocalDateTime.now())
                .build();

        ProofOfDelivery savedPod = proofOfDeliveryRepository.save(pod);
        assignment.setProofOfDelivery(savedPod);

        // Update status to DELIVERED if not already
        if (assignment.getStatus() != AssignmentStatus.DELIVERED) {
            AssignmentStatus prevStatus = assignment.getStatus();
            assignment.setStatus(AssignmentStatus.DELIVERED);
            assignment.setCompletedAt(LocalDateTime.now());
            assignment.getCourier().decrementDeliveries();
            courierRepository.save(assignment.getCourier());
            assignmentRepository.save(assignment);

            eventPublisherService.publishDeliveryStatusUpdated(DeliveryStatusUpdatedEvent.builder()
                    .assignmentId(assignment.getId())
                    .parcelId(assignment.getParcelId())
                    .trackingNumber(assignment.getTrackingNumber())
                    .courierId(assignment.getCourier().getId())
                    .previousStatus(prevStatus.name())
                    .newStatus(AssignmentStatus.DELIVERED.name())
                    .notes("Delivered with Proof of Delivery recorded")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        return mapToProofResponse(savedPod);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchAssignmentResponse> getAllAssignments() {
        return assignmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DispatchAssignmentResponse getAssignmentById(Long id) {
        DispatchAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));
        return mapToResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public DispatchAssignmentResponse getAssignmentByTrackingNumber(String trackingNumber) {
        DispatchAssignment assignment = assignmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found for tracking: " + trackingNumber));
        return mapToResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchAssignmentResponse> getAssignmentsByCourier(Long courierId) {
        return assignmentRepository.findByCourierId(courierId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchAssignmentResponse> getActiveAssignmentsByCourier(Long courierId) {
        return assignmentRepository.findByCourierIdAndStatusIn(courierId, ACTIVE_STATUSES).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProofOfDeliveryResponse getProofOfDelivery(Long assignmentId) {
        ProofOfDelivery pod = proofOfDeliveryRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Proof of delivery not found for assignment: " + assignmentId));
        return mapToProofResponse(pod);
    }

    private DispatchAssignmentResponse mapToResponse(DispatchAssignment a) {
        return DispatchAssignmentResponse.builder()
                .id(a.getId())
                .parcelId(a.getParcelId())
                .courierId(a.getCourier() != null ? a.getCourier().getId() : null)
                .courierName(a.getCourier() != null ? a.getCourier().getFullName() : null)
                .courierPhone(a.getCourier() != null ? a.getCourier().getPhoneNumber() : null)
                .trackingNumber(a.getTrackingNumber())
                .pickupAddress(a.getPickupAddress())
                .deliveryAddress(a.getDeliveryAddress())
                .recipientName(a.getRecipientName())
                .recipientPhone(a.getRecipientPhone())
                .status(a.getStatus())
                .failureReason(a.getFailureReason())
                .notes(a.getNotes())
                .assignedAt(a.getAssignedAt())
                .completedAt(a.getCompletedAt())
                .proofOfDelivery(a.getProofOfDelivery() != null ? mapToProofResponse(a.getProofOfDelivery()) : null)
                .createdAt(a.getCreatedAt())
                .build();
    }

    private ProofOfDeliveryResponse mapToProofResponse(ProofOfDelivery p) {
        return ProofOfDeliveryResponse.builder()
                .id(p.getId())
                .assignmentId(p.getAssignment() != null ? p.getAssignment().getId() : null)
                .recipientName(p.getRecipientName())
                .recipientNic(p.getRecipientNic())
                .signatureUrl(p.getSignatureUrl())
                .photoUrl(p.getPhotoUrl())
                .deliveryNotes(p.getDeliveryNotes())
                .deliveredAt(p.getDeliveredAt())
                .build();
    }
}

