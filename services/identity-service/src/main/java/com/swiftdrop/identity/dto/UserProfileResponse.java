package com.swiftdrop.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private List<String> roles;
    private String status;
    private String merchantApprovalStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
