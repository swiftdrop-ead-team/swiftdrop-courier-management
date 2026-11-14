package com.swiftdrop.parcel.controller;

import com.swiftdrop.parcel.dto.request.CreateParcelRequest;
import com.swiftdrop.parcel.security.UserPrincipal;
import com.swiftdrop.parcel.service.ParcelEstimateService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/parcels/estimate")
public class ParcelEstimateController {
    private final ParcelEstimateService estimates;
    private final com.swiftdrop.parcel.service.MerchantEligibility eligibility;
    public ParcelEstimateController(ParcelEstimateService estimates, com.swiftdrop.parcel.service.MerchantEligibility eligibility) {
        this.estimates = estimates;
        this.eligibility = eligibility;
    }
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','MERCHANT','ADMIN')")
    public ParcelEstimateService.Estimate estimate(@Valid @RequestBody CreateParcelRequest request, @AuthenticationPrincipal UserPrincipal user) {
        eligibility.ensureApproved(user);
        return estimates.estimate(request, user.getId());
    }
}
