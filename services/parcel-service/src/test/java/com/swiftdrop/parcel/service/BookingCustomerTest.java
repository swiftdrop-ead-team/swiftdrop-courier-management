package com.swiftdrop.parcel.service;

import com.swiftdrop.parcel.dto.request.CreateParcelRequest;
import com.swiftdrop.parcel.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BookingCustomerTest {
    private MockServerRestTemplateCustomizer mock;
    private MerchantEligibility eligibility;
    @BeforeEach void setup() {
        mock = new MockServerRestTemplateCustomizer();
        eligibility = new MerchantEligibility(new RestTemplateBuilder().additionalCustomizers(mock), "http://identity");
        var request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
    @AfterEach void cleanup() { RequestContextHolder.resetRequestAttributes(); }
    private UserPrincipal user(String role) { return new UserPrincipal(UUID.randomUUID(), "test@example.com", "Test", List.of(new SimpleGrantedAuthority(role))); }
    @Test void rejectsCustomerTryingToChangeBookingOwner() {
        var request = new CreateParcelRequest(); request.setCustomerId(UUID.randomUUID());
        assertThatThrownBy(() -> eligibility.ensureCustomer(request, user("ROLE_CUSTOMER")))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
    }
    @Test void resolvesActiveCustomerFromIdentityRatherThanTrustingSubmittedEmail() {
        var id = UUID.randomUUID(); var request = new CreateParcelRequest(); request.setCustomerId(id); request.setSenderEmail("forged@example.com");
        mock.getServer().expect(requestTo("http://identity/api/v1/admin/users")).andExpect(header("Authorization", "Bearer test"))
            .andRespond(withSuccess("[{\"id\":\""+id+"\",\"roles\":[\"CUSTOMER\"],\"status\":\"ACTIVE\",\"email\":\"owner@example.com\"}]", MediaType.APPLICATION_JSON));
        eligibility.ensureCustomer(request, user("ROLE_ADMIN"));
        assertThat(request.getSenderEmail()).isEqualTo("owner@example.com"); mock.getServer().verify();
    }
    @Test void rejectsMissingOrInactiveCustomer() {
        var request = new CreateParcelRequest(); request.setCustomerId(UUID.randomUUID());
        mock.getServer().expect(requestTo("http://identity/api/v1/admin/users")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> eligibility.ensureCustomer(request, user("ROLE_ADMIN")))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }
}
