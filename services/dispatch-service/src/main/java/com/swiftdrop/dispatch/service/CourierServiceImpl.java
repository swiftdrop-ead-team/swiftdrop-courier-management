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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourierServiceImpl implements CourierService {

    private static final Logger log = LoggerFactory.getLogger(CourierServiceImpl.class);

    private final CourierRepository courierRepository;

    @Override
    @Transactional
    public CourierResponse registerCourier(CourierCreateRequest request) {
        log.info("Registering courier with userId: {}", request.getUserId());
        if (courierRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new InvalidAssignmentException("Courier with userId '" + request.getUserId() + "' already exists");
        }

        Courier courier = Courier.builder()
                .userId(request.getUserId())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .vehicleType(request.getVehicleType())
                .licensePlate(request.getLicensePlate())
                .assignedZone(request.getAssignedZone() != null ? request.getAssignedZone() : "COLOMBO_CENTRAL")
                .status(CourierStatus.AVAILABLE)
                .maxActiveDeliveries(request.getMaxActiveDeliveries() != null ? request.getMaxActiveDeliveries() : 5)
                .currentActiveDeliveries(0)
                .build();

        Courier saved = courierRepository.save(courier);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CourierResponse getCourierById(Long id) {
        Courier courier = courierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found with id: " + id));
        return mapToResponse(courier);
    }

    @Override
    @Transactional(readOnly = true)
    public CourierResponse getCourierByUserId(String userId) {
        Courier courier = courierRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found with userId: " + userId));
        return mapToResponse(courier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierResponse> getAllCouriers() {
        return courierRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierResponse> getAvailableCouriers() {
        return courierRepository.findAvailableCouriersWithCapacity().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierWorkloadResponse> getCourierWorkloads() {
        return courierRepository.findAll().stream()
                .map(c -> {
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

