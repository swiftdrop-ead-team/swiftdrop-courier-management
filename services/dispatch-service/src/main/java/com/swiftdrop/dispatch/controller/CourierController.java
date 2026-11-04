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

@RestController
@RequestMapping("/api/v1/dispatch/couriers")
@RequiredArgsConstructor
@Tag(name = "Couriers", description = "Courier management and workload distribution endpoints")
public class CourierController {

    private final CourierService courierService;

    @PostMapping
    @Operation(summary = "Register a new courier profile")
    public ResponseEntity<ApiResponse<CourierResponse>> registerCourier(@Valid @RequestBody CourierCreateRequest request) {
        CourierResponse response = courierService.registerCourier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Courier registered successfully", response));
    }

    @GetMapping
    @Operation(summary = "Retrieve all registered couriers")
    public ResponseEntity<ApiResponse<List<CourierResponse>>> getAllCouriers() {
        return ResponseEntity.ok(ApiResponse.ok(courierService.getAllCouriers()));
    }

    @GetMapping("/available")
    @Operation(summary = "Retrieve couriers available for new delivery assignments")
    public ResponseEntity<ApiResponse<List<CourierResponse>>> getAvailableCouriers() {
        return ResponseEntity.ok(ApiResponse.ok(courierService.getAvailableCouriers()));
    }

    @GetMapping("/workloads")
    @Operation(summary = "Retrieve active workload and capacity statistics for all couriers")
    public ResponseEntity<ApiResponse<List<CourierWorkloadResponse>>> getCourierWorkloads() {
        return ResponseEntity.ok(ApiResponse.ok(courierService.getCourierWorkloads()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve courier details by courier ID")
    public ResponseEntity<ApiResponse<CourierResponse>> getCourierById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(courierService.getCourierById(id)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Retrieve courier details by Identity Service user ID")
    public ResponseEntity<ApiResponse<CourierResponse>> getCourierByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(courierService.getCourierByUserId(userId)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update courier operational status (e.g., AVAILABLE, OFFLINE)")
    public ResponseEntity<ApiResponse<CourierResponse>> updateCourierStatus(
            @PathVariable Long id,
            @Valid @RequestBody CourierStatusUpdateRequest request) {
        CourierResponse response = courierService.updateCourierStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Courier status updated successfully", response));
    }
}

