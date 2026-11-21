package com.swiftdrop.parcel.repository;

import com.swiftdrop.parcel.entity.Parcel;
import com.swiftdrop.parcel.enums.ParcelStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ParcelRepository extends JpaRepository<Parcel, UUID>, JpaSpecificationExecutor<Parcel> {
    java.util.Optional<Parcel> findBySenderIdAndMerchantOrderReference(java.util.UUID senderId, String reference);

    Optional<Parcel> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    Page<Parcel> findBySenderId(UUID senderId, Pageable pageable);

    Page<Parcel> findBySenderIdAndStatus(UUID senderId, ParcelStatus status, Pageable pageable);

    Page<Parcel> findByAssignedCourierId(UUID assignedCourierId, Pageable pageable);

    Page<Parcel> findByStatus(ParcelStatus status, Pageable pageable);
}

