package com.swiftdrop.parcel.repository;
import com.swiftdrop.parcel.entity.ParcelEstimate;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ParcelEstimateRepository extends JpaRepository<ParcelEstimate,UUID> {
 @Override @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<ParcelEstimate> findById(UUID id);
}
