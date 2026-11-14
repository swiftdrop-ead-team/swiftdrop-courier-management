package com.swiftdrop.identity.service;

import com.swiftdrop.identity.dto.*;
import com.swiftdrop.identity.entity.*;
import com.swiftdrop.identity.exception.*;
import com.swiftdrop.identity.repository.*;
import com.swiftdrop.identity.security.CookieUtils;
import com.swiftdrop.identity.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MerchantApprovalRecordRepository merchantApprovalRecordRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieUtils cookieUtils;
    private final RateLimiterService rateLimiterService;
    private final PasswordResetMailer resetMailer;

    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request, HttpServletResponse response) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("Passwords do not match");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email address already exists");
        }

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("ROLE_CUSTOMER not initialized"));

        User user = User.builder()
                .email(email)
                .fullName(request.getFullName().trim())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .roles(Collections.singleton(customerRole))
                .build();

        user = userRepository.save(user);

        return buildAuthResponse(user, response, "Customer registered successfully", null);
    }

    @Transactional
    public AuthResponse registerMerchant(MerchantRegisterRequest request, HttpServletResponse response) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("Passwords do not match");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email address already exists");
        }

        Role merchantRole = roleRepository.findByName(RoleName.ROLE_MERCHANT)
                .orElseThrow(() -> new IllegalStateException("ROLE_MERCHANT not initialized"));

        User user = User.builder()
                .email(email)
                .fullName(request.getContactPerson().trim())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .roles(Collections.singleton(merchantRole))
                .build();

        user = userRepository.save(user);

        MerchantApprovalRecord record = MerchantApprovalRecord.builder()
                .user(user)
                .businessName(request.getBusinessName().trim())
                .contactPerson(request.getContactPerson().trim())
                .businessAddress(request.getBusinessAddress().trim())
                .phoneNumber(request.getPhoneNumber())
                .status(MerchantApprovalStatus.PENDING)
                .build();

        merchantApprovalRecordRepository.save(record);

        return buildAuthResponse(user, response, 
                "Merchant registration submitted. Account is pending administrator approval.", 
                MerchantApprovalStatus.PENDING.name());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = request.getEmail().trim().toLowerCase();
        String clientIp = getClientIp(httpRequest);
        String rateLimitKey = email + ":" + clientIp;

        if (rateLimiterService.isBlocked(rateLimitKey)) {
            throw new AuthException("Too many failed login attempts. Please wait 15 minutes before trying again.");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    rateLimiterService.loginFailed(rateLimitKey);
                    return new AuthException("Invalid email or password");
                });

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            rateLimiterService.loginFailed(rateLimitKey);
            throw new AuthException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException("Your account is currently " + user.getStatus().name().toLowerCase() + ". Please contact support.");
        }

        rateLimiterService.loginSucceeded(rateLimitKey);

        String merchantStatus = null;
        if (hasRole(user, RoleName.ROLE_MERCHANT)) {
            merchantStatus = merchantApprovalRecordRepository.findByUserId(user.getId())
                    .map(r -> r.getStatus().name())
                    .orElse(MerchantApprovalStatus.PENDING.name());
        }

        return buildAuthResponse(user, httpResponse, "Login successful", merchantStatus, clientIp, httpRequest.getHeader("User-Agent"));
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken, HttpServletRequest request, HttpServletResponse response) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is missing");
        }

        String hash = jwtService.hashToken(rawRefreshToken);
        RefreshSession session = refreshSessionRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new InvalidTokenException("Invalid or revoked refresh token"));

        if (session.isExpired()) {
            session.setRevoked(true);
            refreshSessionRepository.save(session);
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Rotate token: revoke old session and issue new
        session.setRevoked(true);
        refreshSessionRepository.save(session);

        User user = session.getUser();
        String merchantStatus = null;
        if (hasRole(user, RoleName.ROLE_MERCHANT)) {
            merchantStatus = merchantApprovalRecordRepository.findByUserId(user.getId())
                    .map(r -> r.getStatus().name())
                    .orElse(null);
        }

        return buildAuthResponse(user, response, "Token refreshed successfully", merchantStatus, 
                getClientIp(request), request.getHeader("User-Agent"));
    }

    @Transactional
    public void logout(UUID userId, HttpServletResponse response) {
        if (userId != null) {
            refreshSessionRepository.revokeAllByUserId(userId);
        }
        cookieUtils.clearRefreshTokenCookie(response);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String merchantStatus = null;
        if (hasRole(user, RoleName.ROLE_MERCHANT)) {
            merchantStatus = merchantApprovalRecordRepository.findByUserId(user.getId())
                    .map(r -> r.getStatus().name())
                    .orElse(null);
        }

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name().replace("ROLE_", ""))
                .collect(Collectors.toList());

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .roles(roles)
                .status(user.getStatus().name())
                .merchantApprovalStatus(merchantStatus)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            // Silently return to prevent user enumeration attacks
            return;
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(Instant.now().plusSeconds(3600)) // 1 hour
                .build();

        passwordResetTokenRepository.save(resetToken);
        resetMailer.send(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token"));

        if (resetToken.isExpired()) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Invalidate all active sessions for security
        refreshSessionRepository.revokeAllByUserId(user.getId());
    }

    private AuthResponse buildAuthResponse(User user, HttpServletResponse response, String message, String merchantStatus) {
        return buildAuthResponse(user, response, message, merchantStatus, null, null);
    }

    private AuthResponse buildAuthResponse(User user, HttpServletResponse response, String message, 
                                          String merchantStatus, String ipAddress, String userAgent) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        // Save refresh session in DB
        RefreshSession session = RefreshSession.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(refreshToken))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()))
                .build();
        refreshSessionRepository.save(session);

        // Set HttpOnly cookie
        cookieUtils.addRefreshTokenCookie(response, refreshToken, jwtService.getRefreshExpirationMs());

        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name().replace("ROLE_", ""))
                .collect(Collectors.toList());

        AuthResponse.UserSummary summary = AuthResponse.UserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .roles(roleNames)
                .status(user.getStatus().name())
                .merchantApprovalStatus(merchantStatus)
                .build();

        return AuthResponse.builder()
                .success(true)
                .message(message)
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTimeMs() / 1000)
                .user(summary)
                .build();
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == roleName);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
