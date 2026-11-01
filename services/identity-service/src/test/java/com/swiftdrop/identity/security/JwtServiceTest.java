package com.swiftdrop.identity.security;

import com.swiftdrop.identity.entity.Role;
import com.swiftdrop.identity.entity.RoleName;
import com.swiftdrop.identity.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expiration = 900000;
    private final long refreshExpiration = 604800000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, expiration, refreshExpiration);
    }

    @Test
    void testGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        Role role = Role.builder().name(RoleName.ROLE_CUSTOMER).description("Customer").build();
        User user = User.builder()
                .id(userId)
                .email("test@swiftdrop.io")
                .fullName("Test User")
                .roles(Set.of(role))
                .build();

        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));

        assertEquals(userId, jwtService.getUserIdFromToken(token));
        assertEquals("test@swiftdrop.io", jwtService.getEmailFromToken(token));
        List<String> roles = jwtService.getRolesFromToken(token);
        assertTrue(roles.contains("ROLE_CUSTOMER"));
    }

    @Test
    void testInvalidTokenReturnsFalse() {
        assertFalse(jwtService.validateToken("invalid.token.structure"));
    }

    @Test
    void testHashTokenProducesConsistentSha256() {
        String token = "unique-refresh-token-12345";
        String hash1 = jwtService.hashToken(token);
        String hash2 = jwtService.hashToken(token);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
    }
}
