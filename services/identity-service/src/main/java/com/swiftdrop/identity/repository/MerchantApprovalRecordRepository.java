package com.swiftdrop.identity.repository;

import com.swiftdrop.identity.entity.MerchantApprovalRecord;
import com.swiftdrop.identity.entity.MerchantApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantApprovalRecordRepository extends JpaRepository<MerchantApprovalRecord, UUID> {
    Optional<MerchantApprovalRecord> findByUserId(UUID userId);
    List<MerchantApprovalRecord> findByStatusOrderByCreatedAtDesc(MerchantApprovalStatus status);
}
