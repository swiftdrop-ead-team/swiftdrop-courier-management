package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.dto.*;
import com.swiftdrop.merchant.entity.MerchantApprovalStatus;
import com.swiftdrop.merchant.entity.MerchantProfile;
import com.swiftdrop.merchant.exception.MerchantConflictException;
import com.swiftdrop.merchant.exception.MerchantForbiddenException;
import com.swiftdrop.merchant.exception.MerchantNotFoundException;
import com.swiftdrop.merchant.repository.MerchantProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantProfileService {

    private final MerchantProfileRepository profileRepository;

    @Transactional
    public MerchantProfileResponse register(MerchantPrincipal principal, MerchantRegistrationRequest request) {
        if (principal.apiKey()) {
            throw new MerchantForbiddenException("Merchant API keys cannot register a profile");
        }
        if (profileRepository.findByUserId(principal.userId()).isPresent()) {
            throw new MerchantConflictException("Merchant profile already exists");
        }
        if (principal.email() == null || principal.email().isBlank()) {
            throw new MerchantForbiddenException("Identity token does not contain an email claim");
        }
        MerchantProfile profile = MerchantProfile.builder()
                .userId(principal.userId())
                .email(principal.email())
                .businessName(request.getBusinessName().trim())
                .contactPerson(request.getContactPerson().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .businessAddress(request.getBusinessAddress().trim())
                .approvalStatus(MerchantApprovalStatus.PENDING)
                .build();
        return toResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public MerchantProfileResponse getMyProfile(MerchantPrincipal principal) {
        ensureHumanPrincipal(principal);
        return toResponse(getByUserId(principal.userId()));
    }

    @Transactional
    public MerchantProfileResponse updateMyProfile(
            MerchantPrincipal principal, MerchantProfileUpdateRequest request) {
        ensureHumanPrincipal(principal);
        MerchantProfile profile = getByUserId(principal.userId());
        profile.setBusinessName(request.getBusinessName().trim());
        profile.setContactPerson(request.getContactPerson().trim());
        profile.setPhoneNumber(request.getPhoneNumber().trim());
        profile.setBusinessAddress(request.getBusinessAddress().trim());
        return toResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<MerchantProfileResponse> getPendingMerchants() {
        return profileRepository.findByApprovalStatusOrderByCreatedAtAsc(MerchantApprovalStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public MerchantProfileResponse approve(UUID merchantId, UUID reviewerId, String notes) {
        MerchantProfile profile = getById(merchantId);
        ensurePending(profile);
        profile.setApprovalStatus(MerchantApprovalStatus.APPROVED);
        profile.setReviewNotes(normalizeNotes(notes));
        profile.setReviewedBy(reviewerId);
        profile.setReviewedAt(LocalDateTime.now());
        return toResponse(profileRepository.save(profile));
    }

    @Transactional
    public MerchantProfileResponse reject(UUID merchantId, UUID reviewerId, String notes) {
        MerchantProfile profile = getById(merchantId);
        ensurePending(profile);
        profile.setApprovalStatus(MerchantApprovalStatus.REJECTED);
        profile.setReviewNotes(normalizeNotes(notes));
        profile.setReviewedBy(reviewerId);
        profile.setReviewedAt(LocalDateTime.now());
        return toResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public MerchantProfile requireApproved(MerchantPrincipal principal) {
        MerchantProfile profile = getByUserId(principal.userId());
        ensureApproved(profile);
        return profile;
    }

    @Transactional
    public MerchantProfile requireApprovedForOrder(MerchantPrincipal principal) {
        MerchantProfile profile = profileRepository.findByUserIdForUpdate(principal.userId())
                .orElseThrow(() -> new MerchantNotFoundException("Merchant profile not found"));
        ensureApproved(profile);
        return profile;
    }

    private void ensureApproved(MerchantProfile profile) {
        if (profile.getApprovalStatus() != MerchantApprovalStatus.APPROVED) {
            throw new MerchantForbiddenException("Merchant account is not approved");
        }
    }

    @Transactional(readOnly = true)
    public MerchantProfile getByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant profile not found"));
    }

    @Transactional(readOnly = true)
    public MerchantProfile getById(UUID merchantId) {
        return profileRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant profile not found"));
    }

    private void ensurePending(MerchantProfile profile) {
        if (profile.getApprovalStatus() != MerchantApprovalStatus.PENDING) {
            throw new MerchantConflictException("Only pending merchant registrations can be reviewed");
        }
    }

    private void ensureHumanPrincipal(MerchantPrincipal principal) {
        if (principal.apiKey()) {
            throw new MerchantForbiddenException("Merchant profile requires a Merchant JWT");
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return notes.trim();
    }

    private MerchantProfileResponse toResponse(MerchantProfile profile) {
        return new MerchantProfileResponse(profile.getId(), profile.getUserId(), profile.getEmail(),
                profile.getBusinessName(), profile.getContactPerson(), profile.getPhoneNumber(),
                profile.getBusinessAddress(), profile.getApprovalStatus(), profile.getReviewNotes(),
                profile.getCreatedAt(), profile.getUpdatedAt());
    }
}
