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

/**
 * Implementation of DispatchAssignmentService.
 * Coordinates courier parcel allocation, lifecycle state progression,
 * Proof of Delivery recording, and RabbitMQ event notifications.
 */
@Service
@RequiredArgsConstructor
public class DispatchAssignmentServiceImpl implements DispatchAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(DispatchAssignmentServiceImpl.class);

    private final DispatchAssignmentRepository assignmentRepository;
    private final CourierRepository courierRepository;
    private final ProofOfDeliveryRepository proofOfDeliveryRepository;
    private final EventPublisherService eventPublisherService;

    public DispatchAssignmentServiceImpl(
            DispatchAssignmentRepository assignmentRepository,
            CourierRepository courierRepository,
            ProofOfDeliveryRepository proofOfDeliveryRepository,
            EventPublisherService eventPublisherService) {
        this.assignmentRepository = assignmentRepository;
        this.courierRepository = courierRepository;
        this.proofOfDeliveryRepository = proofOfDeliveryRepository;
        this.eventPublisherService = eventPublisherService;
    }

    // List of active delivery statuses that consume a courier's delivery capacity
    private static final List<AssignmentStatus> ACTIVE_STATUSES = List.of(
            AssignmentStatus.ASSIGNED,
            AssignmentStatus.PICKED_UP,
            AssignmentStatus.IN_TRANSIT,
            AssignmentStatus.OUT_FOR_DELIVERY
    );

    /**
     * Assigns a parcel to an available courier.
     * 1. Checks courier exists in database.
     * 2. Checks courier is not OFFLINE.
     * 3. Checks courier has not exceeded maximum active delivery capacity.
     * 4. Checks parcel is not already actively assigned.
     * 5. Saves assignment, increments courier's active delivery count.
     * 6. Publishes CourierAssignedEvent to RabbitMQ.
     */
    @Override
    @Transactional
    public DispatchAssignmentResponse assignCourier(AssignCourierRequest request) {
        log.info("Assigning parcel: {} to courierId: {}", request.getParcelId(), request.getCourierId());

        // Step 1: Verify courier exists
        Courier courier = courierRepository.findById(request.getCourierId())
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found with id: " + request.getCourierId()));

        // Step 2: Verify courier is online
        if (courier.getStatus() == CourierStatus.OFFLINE) {
            throw new CourierUnavailableException("Courier '" + courier.getFullName() + "' is currently OFFLINE");
        }

        // Step 3: Validate workload capacity (prevents overloading courier)
        if (!courier.canAcceptMoreDeliveries()) {
            throw new CourierOverloadException(String.format(
                    "Courier '%s' has reached maximum active delivery limit (%d/%d)",
                    courier.getFullName(), courier.getCurrentActiveDeliveries(), courier.getMaxActiveDeliveries()
            ));
        }

        // Step 4: Verify parcel is not already assigned and active
        Optional<DispatchAssignment> existingAssignment = assignmentRepository.findByParcelId(request.getParcelId());
        if (existingAssignment.isPresent() && ACTIVE_STATUSES.contains(existingAssignment.get().getStatus())) {
            throw new InvalidAssignmentException("Parcel '" + request.getParcelId() + "' is already assigned to a courier with status: "
                    + existingAssignment.get().getStatus());
        }

        // Step 5: Create and save assignment record
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

        // Increment active delivery count for the courier
        courier.incrementDeliveries();
        courierRepository.save(courier);
        DispatchAssignment savedAssignment = assignmentRepository.save(assignment);

        // Step 6: Publish event to RabbitMQ for Notification Service
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

    /**
     * Updates delivery lifecycle status (e.g. IN_TRANSIT, DELIVERED, FAILED).
     * If reaching terminal status (DELIVERED, FAILED, CANCELLED), decrements courier's active load.
     * Publishes DeliveryStatusUpdatedEvent to RabbitMQ.
     */
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

        // Require failure reason if status is FAILED
        if (newStatus == AssignmentStatus.FAILED) {
            if (request.getFailureReason() == null || request.getFailureReason().isBlank()) {
                throw new InvalidAssignmentException("Failure reason is mandatory when marking delivery as FAILED");
            }
            assignment.setFailureReason(request.getFailureReason());
        }

        // If transitioning from active to terminal state, free up courier capacity
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

    /**
     * Submits Proof of Delivery (POD) when recipient receives package.
     * Records recipient name, signature URL, photo URL.
     * Sets assignment status to DELIVERED and frees courier workload.
     */
    @Override
    @Transactional
    public ProofOfDeliveryResponse submitProofOfDelivery(Long assignmentId, ProofOfDeliveryRequest request) {
        log.info("Submitting proof of delivery for assignmentId: {}", assignmentId);

        DispatchAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        if (assignment.getProofOfDelivery() != null) {
            throw new InvalidAssignmentException("Proof of delivery already recorded for assignment: " + assignmentId);
        }

        // Build and save ProofOfDelivery record
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

        // Complete delivery lifecycle if not already DELIVERED
        if (assignment.getStatus() != AssignmentStatus.DELIVERED) {
            AssignmentStatus prevStatus = assignment.getStatus();
            assignment.setStatus(AssignmentStatus.DELIVERED);
            assignment.setCompletedAt(LocalDateTime.now());
            assignment.getCourier().decrementDeliveries();
            courierRepository.save(assignment.getCourier());
            assignmentRepository.save(assignment);

            // Publish status event to RabbitMQ
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

    /**
     * Retrieves all dispatch assignments.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DispatchAssignmentResponse> getAllAssignments() {
        return assignmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves an assignment by ID.
     */
    @Override
    @Transactional(readOnly = true)
    public DispatchAssignmentResponse getAssignmentById(Long id) {
        DispatchAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));
        return mapToResponse(assignment);
    }

    /**
     * Retrieves an assignment by its tracking number.
     */
    @Override
    @Transactional(readOnly = true)
    public DispatchAssignmentResponse getAssignmentByTrackingNumber(String trackingNumber) {
        DispatchAssignment assignment = assignmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found for tracking: " + trackingNumber));
        return mapToResponse(assignment);
    }

    /**
     * Retrieves all assignments for a courier.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DispatchAssignmentResponse> getAssignmentsByCourier(Long courierId) {
        return assignmentRepository.findByCourierId(courierId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves in-progress / active assignments for a courier.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DispatchAssignmentResponse> getActiveAssignmentsByCourier(Long courierId) {
        return assignmentRepository.findByCourierIdAndStatusIn(courierId, ACTIVE_STATUSES).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves Proof of Delivery details for an assignment.
     */
    @Override
    @Transactional(readOnly = true)
    public ProofOfDeliveryResponse getProofOfDelivery(Long assignmentId) {
        ProofOfDelivery pod = proofOfDeliveryRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Proof of delivery not found for assignment: " + assignmentId));
        return mapToProofResponse(pod);
    }

    /**
     * Helper method to map DispatchAssignment JPA entity to DispatchAssignmentResponse DTO.
     */
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

    /**
     * Helper method to map ProofOfDelivery JPA entity to ProofOfDeliveryResponse DTO.
     */
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
