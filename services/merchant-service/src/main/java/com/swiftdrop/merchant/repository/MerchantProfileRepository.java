package com.swiftdrop.merchant.repository;

import com.swiftdrop.merchant.entity.MerchantApprovalStatus;
import com.swiftdrop.merchant.entity.MerchantProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantProfileRepository extends JpaRepository<MerchantProfile, UUID> {

    Optional<MerchantProfile> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from MerchantProfile p where p.userId = :userId")
    Optional<MerchantProfile> findByUserIdForUpdate(@Param("userId") UUID userId);

    List<MerchantProfile> findByApprovalStatusOrderByCreatedAtAsc(MerchantApprovalStatus status);
}
