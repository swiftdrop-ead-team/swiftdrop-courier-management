package com.swiftdrop.dispatch.service;

import com.swiftdrop.dispatch.dto.*;

import java.util.List;

/**
 * Service interface for dispatch assignments, status updates, and Proof of Delivery.
 */
public interface DispatchAssignmentService {

    // Assigns an unassigned parcel to a courier with capacity validation
    DispatchAssignmentResponse assignCourier(AssignCourierRequest request);

    // Updates delivery status (e.g. IN_TRANSIT, DELIVERED, FAILED)
    DispatchAssignmentResponse updateAssignmentStatus(Long assignmentId, UpdateAssignmentStatusRequest request);

    // Records Proof of Delivery (recipient name, signature/photo URL)
    ProofOfDeliveryResponse submitProofOfDelivery(Long assignmentId, ProofOfDeliveryRequest request);

    // Retrieves all dispatch assignments
    List<DispatchAssignmentResponse> getAllAssignments();

    // Retrieves an assignment by its assignment ID
    DispatchAssignmentResponse getAssignmentById(Long id);

    // Retrieves an assignment by the public tracking number
    DispatchAssignmentResponse getAssignmentByTrackingNumber(String trackingNumber);

    // Retrieves all assignments allocated to a specific courier
    List<DispatchAssignmentResponse> getAssignmentsByCourier(Long courierId);

    // Retrieves active/in-progress assignments for a courier
    List<DispatchAssignmentResponse> getActiveAssignmentsByCourier(Long courierId);

    // Retrieves Proof of Delivery details for an assignment
    ProofOfDeliveryResponse getProofOfDelivery(Long assignmentId);
}
