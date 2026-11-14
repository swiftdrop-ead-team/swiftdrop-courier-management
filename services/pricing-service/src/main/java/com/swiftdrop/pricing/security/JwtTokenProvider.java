package com.swiftdrop.pricing.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Component responsible for parsing, validating, and extracting claims from
 * JWT tokens issued by the SwiftDrop Identity Service.
 *
 * Implements HMAC-SHA verification using the Base64-decoded shared secret key
 * and enforces token issuer validation ("swiftdrop-identity").
 */
@Slf4j
@Component
public class JwtTokenProvider {

    public static final String ISSUER = "swiftdrop-identity";

    private final SecretKey key;

    public JwtTokenProvider(@Value("${swiftdrop.security.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be blank");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validates signature, issuer, and expiration of the provided JWT string.
     * Note: Does not log token or secret value to comply with security constraints.
     *
     * @param token the JWT string
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parses and returns all claims from the signed JWT.
     *
     * @param token the JWT string
     * @return payload claims
     * @throws JwtException if parsing or signature verification fails
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts user ID (UUID) from the "sub" claim.
     */
    public UUID getUserId(String token) {
        Claims claims = getClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts user email from the "email" claim.
     */
    public String getEmail(String token) {
        Claims claims = getClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * Extracts full name from the "fullName" claim.
     */
    public String getFullName(String token) {
        Claims claims = getClaims(token);
        return claims.get("fullName", String.class);
    }

    /**
     * Extracts roles and converts them into Spring Security GrantedAuthorities.
     * Ensures each authority is prefixed with "ROLE_".
     */
    @SuppressWarnings("unchecked")
    public List<SimpleGrantedAuthority> getAuthorities(String token) {
        Claims claims = getClaims(token);
        List<String> roles = claims.get("roles", List.class);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Generates a signed JWT with standard claims matching Identity Service format.
     * Useful for integration and unit testing.
     *
     * @param userId       the user identifier (subject)
     * @param email        the user's email
     * @param fullName     the user's display name
     * @param roles        the role strings (e.g. ROLE_CUSTOMER, ROLE_MERCHANT)
     * @param expirationMs duration in milliseconds before token expires
     * @return serialized compact JWT
     */
    public String generateToken(UUID userId, String email, String fullName, List<String> roles, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("fullName", fullName)
                .claim("roles", roles)
                .issuer(ISSUER)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }
}
