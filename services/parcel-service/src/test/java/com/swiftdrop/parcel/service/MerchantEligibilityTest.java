package com.swiftdrop.parcel.service;

import com.swiftdrop.parcel.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MerchantEligibilityTest {
    private MerchantEligibility eligibility;
    private MockRestServiceServer server;
    private final UUID id = UUID.randomUUID();
    private final UserPrincipal merchant = UserPrincipal.builder().id(id)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))).build();

    @BeforeEach void setup() {
        eligibility = new MerchantEligibility(new RestTemplateBuilder(), "http://identity");
        server = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(eligibility, "client")).build();
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach void cleanup() { RequestContextHolder.resetRequestAttributes(); }

    @Test void approvedMerchantMayBookUsingCurrentIdentityDecision() {
        server.expect(requestTo("http://identity/api/v1/auth/me"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("{\"id\":\"" + id + "\",\"merchantApprovalStatus\":\"APPROVED\"}", MediaType.APPLICATION_JSON));
        assertDoesNotThrow(() -> eligibility.ensureApproved(merchant));
        server.verify();
    }

    @Test void pendingMerchantCannotBookEvenWithValidRole() {
        server.expect(requestTo("http://identity/api/v1/auth/me"))
                .andRespond(withSuccess("{\"id\":\"" + id + "\",\"merchantApprovalStatus\":\"PENDING\"}", MediaType.APPLICATION_JSON));
        var error = assertThrows(ResponseStatusException.class, () -> eligibility.ensureApproved(merchant));
        assertEquals(403, error.getStatusCode().value());
    }

    @Test void identityOutageCannotBypassApproval() {
        server.expect(requestTo("http://identity/api/v1/auth/me")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        var error = assertThrows(ResponseStatusException.class, () -> eligibility.ensureApproved(merchant));
        assertEquals(503, error.getStatusCode().value());
    }
}
