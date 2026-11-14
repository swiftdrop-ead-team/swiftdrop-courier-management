package com.swiftdrop.identity.controller;

import com.swiftdrop.identity.dto.MerchantApprovalActionRequest;
import com.swiftdrop.identity.dto.PendingMerchantResponse;
import com.swiftdrop.identity.dto.UserProfileResponse;
import com.swiftdrop.identity.security.UserPrincipal;
import com.swiftdrop.identity.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    public record AccessRequest(@jakarta.validation.constraints.NotEmpty java.util.Set<@jakarta.validation.constraints.Pattern(regexp="ADMIN|CUSTOMER|COURIER|MERCHANT") String> roles,
        @jakarta.validation.constraints.NotNull com.swiftdrop.identity.entity.UserStatus status) {}
    @PatchMapping("/users/{id}")
    public Map<String,Object> access(@PathVariable UUID id,@jakarta.validation.Valid @RequestBody AccessRequest request,
        @AuthenticationPrincipal UserPrincipal admin) {
        adminService.updateAccess(id,admin.getId(),request.roles().stream().map(r -> com.swiftdrop.identity.entity.RoleName.valueOf("ROLE_"+r)).collect(java.util.stream.Collectors.toSet()),request.status());
        return Map.of("success",true,"message","Account access updated");
    }


    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/merchants/pending")
    public ResponseEntity<List<PendingMerchantResponse>> getPendingMerchants() {
        return ResponseEntity.ok(adminService.getPendingMerchants());
    }

    @PatchMapping("/merchants/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveMerchant(
            @PathVariable("id") UUID recordId,
            @RequestBody(required = false) MerchantApprovalActionRequest request,
            @AuthenticationPrincipal UserPrincipal admin) {
        String notes = request != null ? request.getNotes() : null;
        adminService.approveMerchant(recordId, admin.getId(), notes);
        return ResponseEntity.ok(Map.of("success", true, "message", "Merchant approved successfully"));
    }

    @PatchMapping("/merchants/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectMerchant(
            @PathVariable("id") UUID recordId,
            @RequestBody(required = false) MerchantApprovalActionRequest request,
            @AuthenticationPrincipal UserPrincipal admin) {
        String notes = request != null ? request.getNotes() : null;
        adminService.rejectMerchant(recordId, admin.getId(), notes);
        return ResponseEntity.ok(Map.of("success", true, "message", "Merchant rejected"));
    }
}
