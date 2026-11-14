package com.swiftdrop.pricing.controller;
import com.swiftdrop.pricing.entity.*;
import com.swiftdrop.pricing.repository.PricingRuleRepository;
import com.swiftdrop.pricing.strategy.*;
import com.swiftdrop.pricing.security.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(PricingController.class) @Import(SecurityConfig.class)
@WithMockUser(roles="CUSTOMER")
class PricingControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean PricingRuleRepository rules;
 @MockitoBean PricingStrategyRegistry strategies;
 @MockitoBean JwtService jwt;
 private final String calculation="{\"weightKg\":2,\"lengthCm\":20,\"widthCm\":15,\"heightCm\":10,\"deliveryType\":\"STANDARD\",\"isFragile\":false}";
 @Test void calculateUsesPersistedServiceRates() throws Exception {
  var rule=PricingRule.builder().serviceType(ServiceType.STANDARD).baseFare(new BigDecimal("250"))
   .baseWeightKg(BigDecimal.ONE).perKgRate(new BigDecimal("80")).perKmRate(BigDecimal.ZERO)
   .volumetricDivisor(5000).urgencyMultiplier(BigDecimal.ONE).isActive(true).build();
  when(rules.findByServiceTypeAndIsActiveTrue(ServiceType.STANDARD)).thenReturn(Optional.of(rule));
  when(strategies.getStrategy(ServiceType.STANDARD)).thenReturn(new StandardDeliveryPricingStrategy());
  mvc.perform(post("/api/v1/pricing/calculate").contentType("application/json").content(calculation))
   .andExpect(status().isOk()).andExpect(jsonPath("$.totalFee").value(330)).andExpect(jsonPath("$.currency").value("LKR"));
 }
 @Test void inactiveServiceRejectsBookingEstimate() throws Exception {
  mvc.perform(post("/api/v1/pricing/calculate").contentType("application/json").content(calculation)).andExpect(status().isConflict());
 }
 @Test void customerCannotEditRates() throws Exception {mvc.perform(get("/api/v1/pricing/rules")).andExpect(status().isForbidden());}
 @Test @WithAnonymousUser void anonymousCannotCalculate() throws Exception {
  mvc.perform(post("/api/v1/pricing/calculate").contentType("application/json").content(calculation)).andExpect(status().isUnauthorized());
 }
}
