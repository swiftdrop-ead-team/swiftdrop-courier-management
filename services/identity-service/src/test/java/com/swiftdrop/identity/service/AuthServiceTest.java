package com.swiftdrop.identity.service;

import com.swiftdrop.identity.dto.*;
import com.swiftdrop.identity.entity.*;
import com.swiftdrop.identity.exception.AuthException;
import com.swiftdrop.identity.exception.DuplicateResourceException;
import com.swiftdrop.identity.repository.*;
import com.swiftdrop.identity.security.CookieUtils;
import com.swiftdrop.identity.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MerchantApprovalRecordRepository merchantApprovalRecordRepository;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CookieUtils cookieUtils;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @InjectMocks
    private AuthService authService;

    private Role customerRole;
    private Role merchantRole;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder().id(UUID.randomUUID()).name(RoleName.ROLE_CUSTOMER).description("Customer").build();
        merchantRole = Role.builder().id(UUID.randomUUID()).name(RoleName.ROLE_MERCHANT).description("Merchant").build();
    }

    @Test
    void testRegisterCustomerSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .password("Password123!")
                .confirmPassword("Password123!")
                .acceptTerms(true)
                .build();

        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("mock-jwt-token");
        when(jwtService.generateRefreshToken()).thenReturn("mock-refresh-token");
        when(jwtService.hashToken(anyString())).thenReturn("hashed-refresh-token");
        when(jwtService.getExpirationTimeMs()).thenReturn(900000L);
        when(jwtService.getRefreshExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.registerCustomer(request, httpResponse);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals("john.doe@example.com", response.getUser().getEmail());
        assertTrue(response.getUser().getRoles().contains("CUSTOMER"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsConflict() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("existing@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .acceptTerms(true)
                .build();

        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.registerCustomer(request, httpResponse));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterPasswordMismatchThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("Password123!")
                .confirmPassword("DifferentPassword123!")
                .acceptTerms(true)
                .build();

        assertThrows(AuthException.class, () -> authService.registerCustomer(request, httpResponse));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("Password123!")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .fullName("John Doe")
                .passwordHash("hashedPassword")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build();

        when(rateLimiterService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token-123");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token-123");
        when(jwtService.hashToken(anyString())).thenReturn("hashed-refresh-token");
        when(jwtService.getExpirationTimeMs()).thenReturn(900000L);
        when(jwtService.getRefreshExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.login(request, httpRequest, httpResponse);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("access-token-123", response.getAccessToken());
        verify(rateLimiterService).loginSucceeded(anyString());
    }

    @Test
    void testLoginInvalidPasswordFails() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("WrongPassword!")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .passwordHash("hashedPassword")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build();

        when(rateLimiterService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "hashedPassword")).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.login(request, httpRequest, httpResponse));
        verify(rateLimiterService).loginFailed(anyString());
    }
}
