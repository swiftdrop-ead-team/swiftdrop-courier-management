package com.swiftdrop.parcel.controller;

import com.swiftdrop.parcel.dto.request.CancelParcelRequest;
import com.swiftdrop.parcel.dto.request.CreateParcelRequest;
import com.swiftdrop.parcel.dto.request.UpdateParcelRequest;
import com.swiftdrop.parcel.dto.request.UpdateParcelStatusRequest;
import com.swiftdrop.parcel.dto.response.PageResponse;
import com.swiftdrop.parcel.dto.response.ParcelResponse;
import com.swiftdrop.parcel.dto.response.PublicTrackingResponse;
import com.swiftdrop.parcel.enums.ParcelStatus;
import com.swiftdrop.parcel.security.UserPrincipal;
import com.swiftdrop.parcel.service.ParcelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
public class ParcelController {

    private final ParcelService parcelService;

    /**
     * 1. Create a new parcel consignment
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<ParcelResponse> createParcel(
            @Valid @RequestBody CreateParcelRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("REST: Inbound parcel creation from user [{}]", currentUser.getId());
        ParcelResponse response = parcelService.createParcel(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. Public Tracking endpoint (Open access, privacy-sanitized)
     */
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<PublicTrackingResponse> getPublicTracking(
            @PathVariable String trackingNumber) {

        log.info("REST: Public tracking query for [{}]", trackingNumber);
        PublicTrackingResponse response = parcelService.getPublicTracking(trackingNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. Authenticated full tracking lookup
     */
    @GetMapping("/secure-tracking/{trackingNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ParcelResponse> getSecureTracking(
            @PathVariable String trackingNumber,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ParcelResponse response = parcelService.getParcelByTrackingNumber(trackingNumber, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. Retrieve parcel by unique UUID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ParcelResponse> getParcelById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ParcelResponse response = parcelService.getParcelById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. Retrieve current authenticated user's parcels
     */
    @GetMapping("/my-parcels")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<PageResponse<ParcelResponse>> getMyParcels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<ParcelResponse> response = parcelService.getParcelsForCurrentUser(currentUser, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 6. Admin query: List all parcels with optional status filter
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ParcelResponse>> getAllParcels(
            @RequestParam(required = false) ParcelStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<ParcelResponse> response = parcelService.getAllParcels(status, pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * 7. Query parcels by sender ID
     */
    @GetMapping("/sender/{senderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ParcelResponse>> getParcelsBySender(
            @PathVariable UUID senderId,
            @RequestParam(required = false) ParcelStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<ParcelResponse> response = parcelService.getParcelsBySender(senderId, status, pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * 8. Query parcels by courier ID
     */
    @GetMapping("/courier/{courierId}")
    @PreAuthorize("hasAnyRole('COURIER', 'ADMIN')")
    public ResponseEntity<PageResponse<ParcelResponse>> getParcelsByCourier(
            @PathVariable UUID courierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<ParcelResponse> response = parcelService.getParcelsByCourier(courierId, pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * 9. Update permitted parcel details (while in CREATED status)
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<ParcelResponse> updateParcelDetails(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateParcelRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ParcelResponse response = parcelService.updateParcelDetails(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * 10. Update parcel delivery status (Courier / Admin)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COURIER', 'ADMIN')")
    public ResponseEntity<ParcelResponse> updateParcelStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateParcelStatusRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ParcelResponse response = parcelService.updateParcelStatus(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * 11. Cancel a parcel (Creator / Admin)
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<ParcelResponse> cancelParcel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelParcelRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ParcelResponse response = parcelService.cancelParcel(id, request, currentUser);
        return ResponseEntity.ok(response);
    }
}

