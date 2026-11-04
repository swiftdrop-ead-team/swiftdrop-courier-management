package com.swiftdrop.dispatch.controller;

import com.swiftdrop.dispatch.dto.*;
import com.swiftdrop.dispatch.service.CourierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller exposing HTTP endpoints for courier management and workload balancing.
 * Base route: /api/v1/dispatch/couriers (Routed through API Gateway port 8080 or direct port 8084).
 */
@RestController
@RequestMapping("/api/v1/dispatch/couriers")
@RequiredArgsConstructor
@Tag(name = "Couriers", description = "Courier management and workload distribution endpoints")
public class CourierController {

    private final CourierService courierService;

    /**
     * Registers a new courier profile.
     * HTTP POST /api/v1/dispatch/couriers
     * @param request Validated courier creation payload.
     * @return HTTP 201 Created with the saved courier data.
     */
    @PostMapping
    @Operation(summary = "Register a new courier profile")
    public ResponseEntity<ApiResponse<CourierResponse>> registerCourier(@Valid @RequestBody CourierCreateRequest request) {
        CourierResponse response = courierService.registerCourier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Courier registered successfully", response));
    }

    /**
     * Lists all registered couriers.
     * HTTP GET /api/v1/dispatch/couriers
     */
    @GetMapping
    @Operation(summary = "Retrieve all registered couriers")
    public ResponseEntity<ApiResponse<List<CourierResponse>>> getAllCouriers() {
        return ResponseEntity.ok(ApiResponse.ok(courierService.getAllCouriers()));
    }

    /**
     * Lists only couriers who are AVAILABLE and have remaining delivery capacity.
     * HTTP GET /api/v1/dispatch/couriers/available
     */
    @GetMapping("/available")
    @Operation(summary = "Retrieve couriers available for new delivery assignments")
    public ResponseEntity<ApiResponse<List<CourierResponse>>> getAvailableCouriers() {
        return ResponseEntity.ok(ApiResponse.ok(courierService.getAvailableCouriers()));
    }

    /**
     * Retrieves courier workload metrics and capacity utilization percentages.
     * HTTP GET /api/v1/dispatch/couriers/workloads
     */
    @GetMapping("/workloads")
    @Operation(summary = "Retrieve active workload and capacity statistics for all couriers")
    public ResponseEntity<ApiResponse<List<CourierWorkloadResponse>>> getCourierWorkloads() {
        return ResponseEntity.ok(ApiResponse.ok(courierService.getCourierWorkloads()));
    }

    /**
     * Retrieves courier details by courier database ID.
     * HTTP GET /api/v1/dispatch/couriers/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Retrieve courier details by courier ID")
    public ResponseEntity<ApiResponse<CourierResponse>> getCourierById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(courierService.getCourierById(id)));
    }

    /**
     * Retrieves courier details by Identity Service user ID.
     * HTTP GET /api/v1/dispatch/couriers/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Retrieve courier details by Identity Service user ID")
    public ResponseEntity<ApiResponse<CourierResponse>> getCourierByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(courierService.getCourierByUserId(userId)));
    }

    /**
     * Updates courier operational status (e.g., AVAILABLE, OFFLINE).
     * HTTP PATCH /api/v1/dispatch/couriers/{id}/status
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update courier operational status (e.g., AVAILABLE, OFFLINE)")
    public ResponseEntity<ApiResponse<CourierResponse>> updateCourierStatus(
            @PathVariable Long id,
            @Valid @RequestBody CourierStatusUpdateRequest request) {
        CourierResponse response = courierService.updateCourierStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Courier status updated successfully", response));
    }
}
