package com.swiftdrop.dispatch.controller;
import com.swiftdrop.dispatch.security.*;
import com.swiftdrop.dispatch.service.CourierService;
import com.swiftdrop.dispatch.repository.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(CourierController.class)
@Import({SecurityConfig.class,DispatchAccess.class})
class DispatchSecurityTest {
 @Autowired MockMvc mvc;
 @MockitoBean CourierService service;
 @MockitoBean CourierRepository couriers;
 @MockitoBean DispatchAssignmentRepository assignments;
 @MockitoBean JwtService jwt;
 @Test void anonymousCannotReadDirectory() throws Exception {mvc.perform(get("/api/v1/dispatch/couriers")).andExpect(status().isUnauthorized());}
 @Test @WithMockUser(roles="CUSTOMER") void customerCannotReadDirectory() throws Exception {mvc.perform(get("/api/v1/dispatch/couriers")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="COURIER") void courierCannotReadDirectory() throws Exception {mvc.perform(get("/api/v1/dispatch/couriers")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="COURIER") void courierCannotChangeAnotherProfile() throws Exception {mvc.perform(patch("/api/v1/dispatch/couriers/1/status").contentType("application/json").content("{\"status\":\"OFFLINE\"}")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="ADMIN") void adminCanReadDirectory() throws Exception {mvc.perform(get("/api/v1/dispatch/couriers")).andExpect(status().isOk());}
}
