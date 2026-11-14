package com.swiftdrop.parcel.service;

import com.swiftdrop.parcel.dto.request.CancelParcelRequest;
import com.swiftdrop.parcel.dto.request.CreateParcelRequest;
import com.swiftdrop.parcel.dto.request.UpdateParcelRequest;
import com.swiftdrop.parcel.dto.request.UpdateParcelStatusRequest;
import com.swiftdrop.parcel.dto.response.PageResponse;
import com.swiftdrop.parcel.dto.response.ParcelResponse;
import com.swiftdrop.parcel.dto.response.PublicTrackingResponse;
import com.swiftdrop.parcel.enums.ParcelStatus;
import com.swiftdrop.parcel.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ParcelService {

    ParcelResponse createParcel(CreateParcelRequest request, UserPrincipal currentUser);

    ParcelResponse getParcelById(UUID id, UserPrincipal currentUser);

    ParcelResponse getParcelByTrackingNumber(String trackingNumber, UserPrincipal currentUser);

    PublicTrackingResponse getPublicTracking(String trackingNumber);

    PageResponse<ParcelResponse> getParcelsForCurrentUser(UserPrincipal currentUser, Pageable pageable);

    PageResponse<ParcelResponse> getAllParcels(ParcelStatus status, Pageable pageable, UserPrincipal currentUser);

    PageResponse<ParcelResponse> getParcelsBySender(UUID senderId, ParcelStatus status, Pageable pageable, UserPrincipal currentUser);

    PageResponse<ParcelResponse> getParcelsByCourier(UUID courierId, Pageable pageable, UserPrincipal currentUser);

    ParcelResponse updateParcelDetails(UUID id, UpdateParcelRequest request, UserPrincipal currentUser);

    ParcelResponse updateParcelStatus(UUID id, UpdateParcelStatusRequest request, UserPrincipal currentUser);

    ParcelResponse cancelParcel(UUID id, CancelParcelRequest request, UserPrincipal currentUser);
}

