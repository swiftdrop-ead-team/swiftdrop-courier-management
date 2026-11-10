package com.swiftdrop.tracking.repository;

import com.swiftdrop.tracking.entity.TrackingParcel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrackingParcelRepository extends JpaRepository<TrackingParcel, Long> {
    Optional<TrackingParcel> findByTrackingNumber(String trackingNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TrackingParcel p where p.trackingNumber = :trackingNumber")
    Optional<TrackingParcel> findByTrackingNumberForUpdate(@Param("trackingNumber") String trackingNumber);
}
