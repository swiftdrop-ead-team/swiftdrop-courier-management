package com.swiftdrop.identity.service;

import com.swiftdrop.identity.dto.PendingMerchantResponse;
import com.swiftdrop.identity.dto.UserProfileResponse;
import com.swiftdrop.identity.entity.*;
import com.swiftdrop.identity.exception.ResourceNotFoundException;
import com.swiftdrop.identity.repository.MerchantApprovalRecordRepository;
import com.swiftdrop.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final MerchantApprovalRecordRepository merchantApprovalRecordRepository;

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    String merchantStatus = null;
                    if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_MERCHANT)) {
                        merchantStatus = merchantApprovalRecordRepository.findByUserId(user.getId())
                                .map(r -> r.getStatus().name())
                                .orElse(null);
                    }
                    List<String> roles = user.getRoles().stream()
                            .map(r -> r.getName().name().replace("ROLE_", ""))
                            .collect(Collectors.toList());

                    return UserProfileResponse.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .fullName(user.getFullName())
                            .phoneNumber(user.getPhoneNumber())
                            .roles(roles)
                            .status(user.getStatus().name())
                            .merchantApprovalStatus(merchantStatus)
                            .createdAt(user.getCreatedAt())
                            .updatedAt(user.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PendingMerchantResponse> getPendingMerchants() {
        return merchantApprovalRecordRepository.findByStatusOrderByCreatedAtDesc(MerchantApprovalStatus.PENDING).stream()
                .map(r -> PendingMerchantResponse.builder()
                        .id(r.getId())
                        .userId(r.getUser().getId())
                        .businessName(r.getBusinessName())
                        .contactPerson(r.getContactPerson())
                        .email(r.getUser().getEmail())
                        .phoneNumber(r.getPhoneNumber())
                        .businessAddress(r.getBusinessAddress())
                        .approvalStatus(r.getStatus().name())
                        .notes(r.getNotes())
                        .registeredAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveMerchant(UUID recordId, UUID adminId, String notes) {
        MerchantApprovalRecord record = merchantApprovalRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant approval record not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        record.setStatus(MerchantApprovalStatus.APPROVED);
        record.setReviewer(admin);
        record.setNotes(notes != null ? notes : "Approved by administrator");
        record.setReviewedAt(Instant.now());

        merchantApprovalRecordRepository.save(record);
    }

    @Transactional
    public void rejectMerchant(UUID recordId, UUID adminId, String notes) {
        MerchantApprovalRecord record = merchantApprovalRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant approval record not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        record.setStatus(MerchantApprovalStatus.REJECTED);
        record.setReviewer(admin);
        record.setNotes(notes != null ? notes : "Rejected by administrator");
        record.setReviewedAt(Instant.now());

        merchantApprovalRecordRepository.save(record);
    }
}
