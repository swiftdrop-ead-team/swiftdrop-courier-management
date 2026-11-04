package com.swiftdrop.dispatch.service;

import com.swiftdrop.dispatch.dto.*;

import java.util.List;

public interface DispatchAssignmentService {

    DispatchAssignmentResponse assignCourier(AssignCourierRequest request);

    DispatchAssignmentResponse updateAssignmentStatus(Long assignmentId, UpdateAssignmentStatusRequest request);

    ProofOfDeliveryResponse submitProofOfDelivery(Long assignmentId, ProofOfDeliveryRequest request);

    List<DispatchAssignmentResponse> getAllAssignments();

    DispatchAssignmentResponse getAssignmentById(Long id);

    DispatchAssignmentResponse getAssignmentByTrackingNumber(String trackingNumber);

    List<DispatchAssignmentResponse> getAssignmentsByCourier(Long courierId);

    List<DispatchAssignmentResponse> getActiveAssignmentsByCourier(Long courierId);

    ProofOfDeliveryResponse getProofOfDelivery(Long assignmentId);
}

