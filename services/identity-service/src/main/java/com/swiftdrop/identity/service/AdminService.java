package com.swiftdrop.identity.service;

import com.swiftdrop.identity.dto.PendingMerchantResponse;
import com.swiftdrop.identity.dto.MerchantReviewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    private final com.swiftdrop.identity.repository.RoleRepository roleRepository;
    private final com.swiftdrop.identity.repository.RefreshSessionRepository sessions;
    @Transactional
    public void updateAccess(UUID id,UUID actor,java.util.Set<RoleName> roles,UserStatus status) {
        if(id.equals(actor) && (status!=UserStatus.ACTIVE || !roles.contains(RoleName.ROLE_ADMIN)))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"You cannot remove your own administrator access");
        var user=userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        // Merchant approval remains a separate workflow; role edits do not provision merchant records.
        if(roles.contains(RoleName.ROLE_MERCHANT) && user.getMerchantRecord()==null)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Register a merchant account before assigning the merchant role");
        user.setRoles(roles.stream().map(r -> roleRepository.findByName(r).orElseThrow()).collect(java.util.stream.Collectors.toSet()));
        user.setStatus(status);userRepository.save(user);sessions.revokeAllByUserId(id);
    }

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

    @Transactional(readOnly = true)
    public List<MerchantReviewResponse> getMerchantReviews() {
        return merchantApprovalRecordRepository.findByReviewedAtIsNotNullOrderByReviewedAtDescIdDesc().stream()
                .map(r -> new MerchantReviewResponse(r.getId(), r.getBusinessName(), r.getStatus(),
                        r.getNotes() == null ? "" : r.getNotes(), r.getReviewedAt(),
                        r.getReviewer() == null ? "Reviewer unavailable" : r.getReviewer().getFullName()))
                .toList();
    }

    @Transactional
    public void approveMerchant(UUID recordId, UUID adminId, String notes) {
        review(recordId, adminId, MerchantApprovalStatus.APPROVED, notes == null ? "Approved by administrator" : notes);
    }

    @Transactional
    public void rejectMerchant(UUID recordId, UUID adminId, String notes) {
        review(recordId, adminId, MerchantApprovalStatus.REJECTED, notes == null ? "Rejected by administrator" : notes);
    }

    private void review(UUID recordId, UUID adminId, MerchantApprovalStatus decision, String notes) {
        MerchantApprovalRecord record = merchantApprovalRecordRepository.findForReview(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant approval record not found"));
        if (record.getStatus() != MerchantApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application has already been reviewed.");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        record.setStatus(decision);
        record.setReviewer(admin);
        record.setNotes(notes);
        record.setReviewedAt(Instant.now());

        merchantApprovalRecordRepository.save(record);
    }
}
