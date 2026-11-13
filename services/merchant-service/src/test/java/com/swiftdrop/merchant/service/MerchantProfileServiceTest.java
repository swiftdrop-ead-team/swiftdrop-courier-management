package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.dto.MerchantPrincipal;
import com.swiftdrop.merchant.dto.MerchantRegistrationRequest;
import com.swiftdrop.merchant.entity.MerchantApprovalStatus;
import com.swiftdrop.merchant.entity.MerchantProfile;
import com.swiftdrop.merchant.exception.MerchantConflictException;
import com.swiftdrop.merchant.exception.MerchantForbiddenException;
import com.swiftdrop.merchant.repository.MerchantProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantProfileServiceTest {

    @Mock
    private MerchantProfileRepository profileRepository;

    @InjectMocks
    private MerchantProfileService profileService;

    private UUID userId;
    private MerchantPrincipal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        principal = new MerchantPrincipal(userId, "merchant@example.test", false);
    }

    @Test
    void registrationCreatesPendingProfileTiedToIdentityUser() {
        MerchantRegistrationRequest request = MerchantRegistrationRequest.builder()
                .businessName(" Northwind ")
                .contactPerson(" Ada Merchant ")
                .phoneNumber("+94770000000")
                .businessAddress("1 Market Road")
                .build();
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(profileRepository.save(any(MerchantProfile.class))).thenAnswer(call -> {
            MerchantProfile saved = call.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var response = profileService.register(principal, request);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("merchant@example.test");
        assertThat(response.businessName()).isEqualTo("Northwind");
        assertThat(response.approvalStatus()).isEqualTo(MerchantApprovalStatus.PENDING);
    }

    @Test
    void duplicateRegistrationIsRejected() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(MerchantProfile.builder().build()));
        assertThatThrownBy(() -> profileService.register(principal, MerchantRegistrationRequest.builder().build()))
                .isInstanceOf(MerchantConflictException.class);
        verify(profileRepository, never()).save(any());
    }

    @Test
    void unapprovedMerchantCannotUseApprovedResources() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(MerchantProfile.builder()
                .userId(userId).approvalStatus(MerchantApprovalStatus.PENDING).build()));
        assertThatThrownBy(() -> profileService.requireApproved(principal))
                .isInstanceOf(MerchantForbiddenException.class)
                .hasMessageContaining("not approved");
    }

    @Test
    void onlyPendingMerchantCanBeReviewed() {
        UUID merchantId = UUID.randomUUID();
        when(profileRepository.findById(merchantId)).thenReturn(Optional.of(MerchantProfile.builder()
                .id(merchantId).approvalStatus(MerchantApprovalStatus.REJECTED).build()));
        assertThatThrownBy(() -> profileService.approve(merchantId, UUID.randomUUID(), null))
                .isInstanceOf(MerchantConflictException.class);
    }

    @Test
    void pendingMerchantCanBeRejectedByReviewer() {
        UUID merchantId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        MerchantProfile profile = MerchantProfile.builder()
                .id(merchantId)
                .approvalStatus(MerchantApprovalStatus.PENDING)
                .build();
        when(profileRepository.findById(merchantId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);

        var response = profileService.reject(merchantId, reviewerId, "Documents incomplete");

        assertThat(response.approvalStatus()).isEqualTo(MerchantApprovalStatus.REJECTED);
        assertThat(profile.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(response.reviewNotes()).isEqualTo("Documents incomplete");
    }
}
