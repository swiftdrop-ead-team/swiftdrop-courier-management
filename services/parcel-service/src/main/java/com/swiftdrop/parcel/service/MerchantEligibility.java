package com.swiftdrop.parcel.service;

import com.swiftdrop.parcel.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

/** Read the current approval decision; a stale access token must not bypass review. */
@Service
public class MerchantEligibility {
    private final RestTemplate client;
    private final String identityUrl;

    public MerchantEligibility(RestTemplateBuilder builder,
            @Value("${IDENTITY_SERVICE_URL:http://localhost:8081}") String identityUrl) {
        this.client = builder.connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5)).build();
        this.identityUrl = identityUrl;
    }

    public void ensureApproved(UserPrincipal user) {
        boolean merchant = user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MERCHANT".equals(a.getAuthority()));
        boolean admin = user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!merchant || admin) return;

        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servlet)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Merchant approval is required");
        }
        var headers = new HttpHeaders();
        headers.set("Authorization", servlet.getRequest().getHeader("Authorization"));
        try {
            Map<?, ?> profile = client.exchange(identityUrl + "/api/v1/auth/me", HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class).getBody();
            if (profile == null || !user.getId().toString().equals(profile.get("id"))
                    || !"APPROVED".equals(profile.get("merchantApprovalStatus"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Your business must be approved before creating deliveries");
            }
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account access is required");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Merchant approval could not be verified. Please try again.");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Merchant approval could not be verified. Please try again.");
        }
    }
}
