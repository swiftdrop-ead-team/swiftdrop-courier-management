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

@RestController
@RequestMapping("/api/v1/dispatch/assignments")
@RequiredArgsConstructor
@Tag(name = "Dispatch Assignments", description = "Courier parcel assignment, lifecycle updates, and Proof of Delivery endpoints")
public class DispatchAssignmentController {

    private final DispatchAssignmentService assignmentService;

    @PostMapping
    @Operation(summary = "Assign a parcel to an available courier")
    public ResponseEntity<ApiResponse<DispatchAssignmentResponse>> assignCourier(
            @Valid @RequestBody AssignCourierRequest request) {
        DispatchAssignmentResponse response = assignmentService.assignCourier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Parcel assigned to courier successfully", response));
    }

    @GetMapping
    @Operation(summary = "Retrieve all dispatch assignments")
    public ResponseEntity<ApiResponse<List<DispatchAssignmentResponse>>> getAllAssignments() {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getAllAssignments()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve dispatch assignment by assignment ID")
    public ResponseEntity<ApiResponse<DispatchAssignmentResponse>> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getAssignmentById(id)));
    }

    @GetMapping("/tracking/{trackingNumber}")
    @Operation(summary = "Retrieve dispatch assignment by parcel tracking number")
    public ResponseEntity<ApiResponse<DispatchAssignmentResponse>> getAssignmentByTrackingNumber(
            @PathVariable String trackingNumber) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getAssignmentByTrackingNumber(trackingNumber)));
    }

    @GetMapping("/courier/{courierId}")
    @Operation(summary = "Retrieve all dispatch assignments for a specific courier")
    public ResponseEntity<ApiResponse<List<DispatchAssignmentResponse>>> getAssignmentsByCourier(
            @PathVariable Long courierId) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getAssignmentsByCourier(courierId)));
    }

    @GetMapping("/courier/{courierId}/active")
    @Operation(summary = "Retrieve active/in-progress dispatch assignments for a courier")
    public ResponseEntity<ApiResponse<List<DispatchAssignmentResponse>>> getActiveAssignmentsByCourier(
            @PathVariable Long courierId) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getActiveAssignmentsByCourier(courierId)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update assignment delivery status (PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, FAILED, etc.)")
    public ResponseEntity<ApiResponse<DispatchAssignmentResponse>> updateAssignmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssignmentStatusRequest request) {
        DispatchAssignmentResponse response = assignmentService.updateAssignmentStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Assignment status updated successfully", response));
    }

    @PostMapping("/{id}/proof")
    @Operation(summary = "Submit Proof of Delivery (POD) for an assignment")
    public ResponseEntity<ApiResponse<ProofOfDeliveryResponse>> submitProofOfDelivery(
            @PathVariable Long id,
            @Valid @RequestBody ProofOfDeliveryRequest request) {
        ProofOfDeliveryResponse response = assignmentService.submitProofOfDelivery(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Proof of delivery submitted successfully", response));
    }

    @GetMapping("/{id}/proof")
    @Operation(summary = "Retrieve Proof of Delivery for an assignment")
    public ResponseEntity<ApiResponse<ProofOfDeliveryResponse>> getProofOfDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.getProofOfDelivery(id)));
    }
}

