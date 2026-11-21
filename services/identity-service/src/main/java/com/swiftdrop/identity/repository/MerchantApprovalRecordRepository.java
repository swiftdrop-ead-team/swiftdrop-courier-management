package com.swiftdrop.identity.repository;

import com.swiftdrop.identity.entity.MerchantApprovalRecord;
import com.swiftdrop.identity.entity.MerchantApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantApprovalRecordRepository extends JpaRepository<MerchantApprovalRecord, UUID> {
    Optional<MerchantApprovalRecord> findByUserId(UUID userId);
    List<MerchantApprovalRecord> findByStatusOrderByCreatedAtDesc(MerchantApprovalStatus status);

    @EntityGraph(attributePaths = "reviewer")
    List<MerchantApprovalRecord> findByReviewedAtIsNotNullOrderByReviewedAtDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from MerchantApprovalRecord r where r.id = :id")
    Optional<MerchantApprovalRecord> findForReview(@Param("id") UUID id);
}
