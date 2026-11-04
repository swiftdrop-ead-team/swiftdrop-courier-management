package com.swiftdrop.dispatch.service;

import com.swiftdrop.dispatch.dto.CourierCreateRequest;
import com.swiftdrop.dispatch.dto.CourierResponse;
import com.swiftdrop.dispatch.dto.CourierStatusUpdateRequest;
import com.swiftdrop.dispatch.dto.CourierWorkloadResponse;

import java.util.List;

/**
 * Service interface for courier fleet management operations.
 */
public interface CourierService {

    // Registers a new courier profile in the database
    CourierResponse registerCourier(CourierCreateRequest request);

    // Retrieves a courier profile by courier database ID
    CourierResponse getCourierById(Long id);

    // Retrieves a courier profile by their Identity Service user ID
    CourierResponse getCourierByUserId(String userId);

    // Returns a list of all registered couriers
    List<CourierResponse> getAllCouriers();

    // Returns couriers who are AVAILABLE and have free capacity
    List<CourierResponse> getAvailableCouriers();

    // Returns active delivery workloads and capacity utilization for all couriers
    List<CourierWorkloadResponse> getCourierWorkloads();

    // Updates courier operational status (e.g. to OFFLINE or AVAILABLE)
    CourierResponse updateCourierStatus(Long id, CourierStatusUpdateRequest request);
}
