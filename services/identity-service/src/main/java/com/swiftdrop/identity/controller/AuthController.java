package com.swiftdrop.identity.controller;

import com.swiftdrop.identity.dto.*;
import com.swiftdrop.identity.security.CookieUtils;
import com.swiftdrop.identity.security.UserPrincipal;
import com.swiftdrop.identity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtils cookieUtils;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerCustomer(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.registerCustomer(request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/merchant/register")
    public ResponseEntity<AuthResponse> registerMerchant(
            @Valid @RequestBody MerchantRegisterRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.registerMerchant(request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResponse authResponse = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) TokenRefreshRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = null;
        if (body != null && body.getRefreshToken() != null && !body.getRefreshToken().isBlank()) {
            refreshToken = body.getRefreshToken();
        } else {
            refreshToken = cookieUtils.getRefreshTokenFromCookies(request).orElse(null);
        }

        AuthResponse authResponse = authService.refreshToken(refreshToken, request, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletResponse response) {
        UUID userId = principal != null ? principal.getId() : null;
        authService.logout(userId, response);
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserProfileResponse profile = authService.getMe(principal.getId());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "If this email exists in our system, password reset instructions have been dispatched"
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password has been successfully updated"
        ));
    }
}
