package com.swiftdrop.pricing.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    // 48-byte Base64-encoded secret suitable for HS384
    private static final String TEST_SECRET = "x2oOe75bdj0snm7uNSxxn7cUXom9L68cltwvoVp1sk6H4Ad0u+lNjOqno+AuKTTn";
    // Different secret for testing invalid signature
    private static final String OTHER_SECRET = "dGhpc0lzQURpZmZlcmVudFNlY3JldEtleUZvclRlc3Rpbmc4OTAxMjM0NTY3ODkwMTI=";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET);
    }

    @Test
    @DisplayName("Should successfully generate and validate a valid token")
    void testValidateValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(
                userId, "test@swiftdrop.io", "Test User", List.of("ROLE_CUSTOMER"), 60000);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(userId, jwtTokenProvider.getUserId(token));
        assertEquals("test@swiftdrop.io", jwtTokenProvider.getEmail(token));
        assertEquals("Test User", jwtTokenProvider.getFullName(token));

        List<SimpleGrantedAuthority> authorities = jwtTokenProvider.getAuthorities(token);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_CUSTOMER", authorities.get(0).getAuthority());
    }

    @Test
    @DisplayName("Should reject expired token")
    void testValidateExpiredToken() {
        UUID userId = UUID.randomUUID();
        // Negative expiration duration -> already expired
        String token = jwtTokenProvider.generateToken(
                userId, "test@swiftdrop.io", "Test User", List.of("ROLE_CUSTOMER"), -1000);

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should reject token signed with a different secret key")
    void testValidateInvalidSignature() {
        UUID userId = UUID.randomUUID();
        byte[] keyBytes = Decoders.BASE64.decode(OTHER_SECRET);
        SecretKey otherKey = Keys.hmacShaKeyFor(keyBytes);

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("email", "bad@swiftdrop.io")
                .claim("roles", List.of("ROLE_CUSTOMER"))
                .issuer(JwtTokenProvider.ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(otherKey)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should reject token with untrusted issuer")
    void testValidateWrongIssuer() {
        UUID userId = UUID.randomUUID();
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("email", "test@swiftdrop.io")
                .claim("roles", List.of("ROLE_CUSTOMER"))
                .issuer("untrusted-issuer")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should reject malformed token strings")
    void testValidateMalformedToken() {
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.jwt.token"));
        assertFalse(jwtTokenProvider.validateToken("eyJhbGciOiJIUzM4NCJ9.invalidpayload.invalidsignature"));
    }

    @Test
    @DisplayName("Should reject null or blank tokens")
    void testValidateNullOrBlankToken() {
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken("   "));
    }

    @Test
    @DisplayName("Should correctly extract multiple roles and prefix with ROLE_ if missing")
    void testExtractAuthoritiesMultipleRoles() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(
                userId, "merchant@swiftdrop.io", "Merchant User",
                List.of("MERCHANT", "ROLE_ADMIN"), 60000);

        List<SimpleGrantedAuthority> authorities = jwtTokenProvider.getAuthorities(token);
        assertEquals(2, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_MERCHANT")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Should throw exception when constructed with null or blank secret")
    void testConstructorBlankSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(null));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider("   "));
    }
}
