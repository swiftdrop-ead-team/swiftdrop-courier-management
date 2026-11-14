package com.swiftdrop.dispatch.service;

import com.swiftdrop.dispatch.dto.CourierCreateRequest;
import com.swiftdrop.dispatch.dto.CourierResponse;
import com.swiftdrop.dispatch.dto.CourierStatusUpdateRequest;
import com.swiftdrop.dispatch.dto.CourierWorkloadResponse;
import com.swiftdrop.dispatch.entity.Courier;
import com.swiftdrop.dispatch.entity.CourierStatus;
import com.swiftdrop.dispatch.exception.InvalidAssignmentException;
import com.swiftdrop.dispatch.exception.ResourceNotFoundException;
import com.swiftdrop.dispatch.repository.CourierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of CourierService containing business logic for courier management.
 */
@Service
public class CourierServiceImpl implements CourierService {

    private static final Logger log = LoggerFactory.getLogger(CourierServiceImpl.class);

    private final CourierRepository courierRepository;

    public CourierServiceImpl(CourierRepository courierRepository) {
        this.courierRepository = courierRepository;
    }

    /**
     * Registers a new courier in the system.
     * Validates that the userId is not already taken.
     */
    @Override
    @Transactional
    public CourierResponse registerCourier(CourierCreateRequest request) {
        log.info("Registering courier with userId: {}", request.getUserId());

        // Check if a courier with this userId already exists
        if (courierRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new InvalidAssignmentException("Courier with userId '" + request.getUserId() + "' already exists");
        }

        // Build new Courier entity with default available status
        Courier courier = Courier.builder()
                .userId(request.getUserId()).email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .vehicleType(request.getVehicleType())
                .licensePlate(request.getLicensePlate())
                .assignedZone(request.getAssignedZone() != null ? request.getAssignedZone() : "COLOMBO_CENTRAL")
                .status(CourierStatus.AVAILABLE)
                .maxActiveDeliveries(request.getMaxActiveDeliveries() != null ? request.getMaxActiveDeliveries() : 5)
                .currentActiveDeliveries(0)
                .build();

        // Persist to PostgreSQL database
        Courier saved = courierRepository.save(courier);
        return mapToResponse(saved);
    }

    /**
     * Retrieves courier details by database ID.
     */
    @Override
    @Transactional(readOnly = true)
    public CourierResponse getCourierById(Long id) {
        Courier courier = courierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found with id: " + id));
        return mapToResponse(courier);
    }

    /**
     * Retrieves courier details by user account ID.
     */
    @Override
    @Transactional(readOnly = true)
    public CourierResponse getCourierByUserId(String userId) {
        Courier courier = courierRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found with userId: " + userId));
        return mapToResponse(courier);
    }

    /**
     * Returns all registered couriers in the system.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CourierResponse> getAllCouriers() {
        return courierRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Returns only couriers who are AVAILABLE and below their maximum active workload.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CourierResponse> getAvailableCouriers() {
        return courierRepository.findAvailableCouriersWithCapacity().stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Computes real-time workload statistics for all couriers.
     * Calculates capacity utilization percentage (e.g. 2 active / 5 max = 40.0%).
     */
    @Override
    @Transactional(readOnly = true)
    public List<CourierWorkloadResponse> getCourierWorkloads() {
        return courierRepository.findAll().stream()
                .map(c -> {
                    // Calculate percentage of capacity utilized
                    double utilization = c.getMaxActiveDeliveries() > 0
                            ? ((double) c.getCurrentActiveDeliveries() / c.getMaxActiveDeliveries()) * 100.0
                            : 0.0;

                    return CourierWorkloadResponse.builder()
                            .courierId(c.getId())
                            .courierName(c.getFullName())
                            .phoneNumber(c.getPhoneNumber())
                            .vehicleType(c.getVehicleType())
                            .assignedZone(c.getAssignedZone())
                            .status(c.getStatus())
                            .activeDeliveries(c.getCurrentActiveDeliveries())
                            .maxDeliveries(c.getMaxActiveDeliveries())
                            .utilizationPercentage(Math.round(utilization * 10.0) / 10.0)
                            .canAcceptMore(c.canAcceptMoreDeliveries())
                            .build();
                })
                .toList();
    }

    /**
     * Updates courier operational status (e.g. from AVAILABLE to OFFLINE).
     */
    @Override
    @Transactional
    public CourierResponse updateCourierStatus(Long id, CourierStatusUpdateRequest request) {
        Courier courier = courierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found with id: " + id));

        log.info("Updating courier id: {} status from {} to {}", id, courier.getStatus(), request.getStatus());
        courier.setStatus(request.getStatus());
        Courier updated = courierRepository.save(courier);
        return mapToResponse(updated);
    }

    /**
     * Helper method to map Courier JPA entity to CourierResponse DTO.
     */
    private CourierResponse mapToResponse(Courier c) {
        return CourierResponse.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .fullName(c.getFullName())
                .phoneNumber(c.getPhoneNumber())
                .vehicleType(c.getVehicleType())
                .licensePlate(c.getLicensePlate())
                .assignedZone(c.getAssignedZone())
                .status(c.getStatus())
                .maxActiveDeliveries(c.getMaxActiveDeliveries())
                .currentActiveDeliveries(c.getCurrentActiveDeliveries())
                .availableForAssignment(c.canAcceptMoreDeliveries())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
