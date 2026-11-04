package com.swiftdrop.dispatch.service;

import com.swiftdrop.dispatch.dto.CourierCreateRequest;
import com.swiftdrop.dispatch.dto.CourierResponse;
import com.swiftdrop.dispatch.dto.CourierStatusUpdateRequest;
import com.swiftdrop.dispatch.dto.CourierWorkloadResponse;

import java.util.List;

public interface CourierService {

    CourierResponse registerCourier(CourierCreateRequest request);

    CourierResponse getCourierById(Long id);

    CourierResponse getCourierByUserId(String userId);

    List<CourierResponse> getAllCouriers();

    List<CourierResponse> getAvailableCouriers();

    List<CourierWorkloadResponse> getCourierWorkloads();

    CourierResponse updateCourierStatus(Long id, CourierStatusUpdateRequest request);
}

