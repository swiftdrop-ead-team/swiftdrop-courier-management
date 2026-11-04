package com.swiftdrop.dispatch.controller;

import com.swiftdrop.dispatch.dto.*;
import com.swiftdrop.dispatch.service.DispatchAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller exposing HTTP endpoints for parcel assignments, delivery tracking,
 * status transitions, and Proof of Delivery (POD) registration.
 * Base route: /api/v1/dispatch/assignments
 */
@RestController
@RequestMapping("/api/v1/dispatch/assignments")
@RequiredArgsConstructor
@Tag(name = "Dispatch Assignments", description = "Courier parcel assignment, lifecycle updates, and Proof of Delivery endpoints")
public class DispatchAssignmentController {

    private final DispatchAssignmentService assignmentService;

    /**
     * Assigns a parcel to an available courier.
     * HTTP POST /api/v1/dispatch/assignments
     * Validates capacity, increments courier load, and publishes RabbitMQ event.
     */
    @PostMapping
    @Operation(summary = "Assign a parcel to an available courier")
    public ResponseEntity<ApiResponse<DispatchAssignmentResponse>> assignCourier(
            @Valid @RequestBody AssignCourierRequest request) {
        DispatchAssignmentResponse response = assignmentService.assignCourier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Parcel assigned to courier successfully", response));
    }

    /**
     * Lists all dispatch assignments.
     * HTTP GET /api/v1/dispatch/assignments
     */
    @GetMapping
    @Operation(summary = "Retrieve all dispatch assignments")
    public ResponseEntity<ApiResponse<List<DispatchAssignmentResponse>>> getAllAssignments() {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getAllAssignments()));
    }

    /**
     * Retrieves a single assignment by assignment ID.
     * HTTP GET /api/v1/dispatch/assignments/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Retrieve dispatch assignment by assignment ID")
    public ResponseEntity<ApiResponse<DispatchAssignmentResponse>> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getAssignmentById(id)));
    }

    /**
     * Retrieves an assignment by parcel tracking number (e.g., SWIFT-COL-1029).
     * HTTP GET /api/v1/dispatch/assignments/tracking/{trackingNumber}
     */
    @GetMapping("/tracking/{trackingNumber}")
    @Operation(summary = "Retrieve dispatch assignment by parcel tracking number")
    public ResponseEntity<ApiResponse<DispatchAssignmentResponse>> getAssignmentByTrackingNumber(
            @PathVariable String trackingNumber) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getAssignmentByTrackingNumber(trackingNumber)));
    }

    /**
     * Retrieves all assignments assigned to a specific courier.
     * HTTP GET /api/v1/dispatch/assignments/courier/{courierId}
     */
    @GetMapping("/courier/{courierId}")
    @Operation(summary = "Retrieve all dispatch assignments for a specific courier")
    public ResponseEntity<ApiResponse<List<DispatchAssignmentResponse>>> getAssignmentsByCourier(
            @PathVariable Long courierId) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getAssignmentsByCourier(courierId)));
    }

    /**
     * Retrieves only currently active/in-progress deliveries for a courier.
     * HTTP GET /api/v1/dispatch/assignments/courier/{courierId}/active
     */
    @GetMapping("/courier/{courierId}/active")
    @Operation(summary = "Retrieve active/in-progress dispatch assignments for a courier")
    public ResponseEntity<ApiResponse<List<DispatchAssignmentResponse>>> getActiveAssignmentsByCourier(
            @PathVariable Long courierId) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getActiveAssignmentsByCourier(courierId)));
    }

    /**
     * Updates delivery lifecycle status (PICKED_UP, IN_TRANSIT, DELIVERED, FAILED).
     * HTTP PATCH /api/v1/dispatch/assignments/{id}/status
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update assignment delivery status (PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, FAILED, etc.)")
    public ResponseEntity<ApiResponse<DispatchAssignmentResponse>> updateAssignmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssignmentStatusRequest request) {
        DispatchAssignmentResponse response = assignmentService.updateAssignmentStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Assignment status updated successfully", response));
    }

    /**
     * Submits Proof of Delivery (POD) for an assignment with recipient name and signature/photo.
     * HTTP POST /api/v1/dispatch/assignments/{id}/proof
     */
    @PostMapping("/{id}/proof")
    @Operation(summary = "Submit Proof of Delivery (POD) for an assignment")
    public ResponseEntity<ApiResponse<ProofOfDeliveryResponse>> submitProofOfDelivery(
            @PathVariable Long id,
            @Valid @RequestBody ProofOfDeliveryRequest request) {
        ProofOfDeliveryResponse response = assignmentService.submitProofOfDelivery(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Proof of delivery submitted successfully", response));
    }

    /**
     * Retrieves the Proof of Delivery details for an assignment.
     * HTTP GET /api/v1/dispatch/assignments/{id}/proof
     */
    @GetMapping("/{id}/proof")
    @Operation(summary = "Retrieve Proof of Delivery for an assignment")
    public ResponseEntity<ApiResponse<ProofOfDeliveryResponse>> getProofOfDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getProofOfDelivery(id)));
    }
}
