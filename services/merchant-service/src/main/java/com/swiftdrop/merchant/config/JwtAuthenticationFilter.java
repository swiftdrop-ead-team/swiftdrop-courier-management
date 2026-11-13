package com.swiftdrop.merchant.config;

import com.swiftdrop.merchant.dto.MerchantPrincipal;
import com.swiftdrop.merchant.entity.MerchantApprovalStatus;
import com.swiftdrop.merchant.entity.MerchantApiKey;
import com.swiftdrop.merchant.repository.MerchantApiKeyRepository;
import com.swiftdrop.merchant.repository.MerchantProfileRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String IDENTITY_ISSUER = "swiftdrop-identity";
    private static final String API_KEY_PREFIX = "sd_live_";
    private static final int API_KEY_LENGTH = 51;
    private static final int API_KEY_PREFIX_LENGTH = 16;
    private static final Set<String> IDENTITY_ROLES = Set.of(
            "ROLE_CUSTOMER", "ROLE_MERCHANT", "ROLE_COURIER", "ROLE_ADMIN");
    private static final Set<String> API_KEY_SCOPES = Set.of(
            "orders:read", "orders:write", "dashboard:read");
    private static final List<String> MERCHANT_JWT_SCOPES = List.of(
            "orders:read", "orders:write", "dashboard:read");

    private final SecretKey key;
    private final MerchantApiKeyRepository apiKeyRepository;
    private final MerchantProfileRepository profileRepository;

    public JwtAuthenticationFilter(
            @Value("${swiftdrop.security.jwt.secret}") String secret,
            MerchantApiKeyRepository apiKeyRepository,
            MerchantProfileRepository profileRepository) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.apiKeyRepository = apiKeyRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        String apiKey = request.getHeader("X-API-Key");
        if (StringUtils.hasText(apiKey)) {
            authenticateApiKey(apiKey);
        } else {
            authenticateJwt(request.getHeader("Authorization"));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateJwt(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)
                || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return;
        }
        String token = authorizationHeader.substring(7);
        if (!StringUtils.hasText(token) || token.chars().anyMatch(Character::isWhitespace)) {
            return;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(IDENTITY_ISSUER).build()
                    .parseSignedClaims(token).getPayload();
            if (!StringUtils.hasText(claims.getSubject())
                    || !(claims.get("email") instanceof String email)
                    || !StringUtils.hasText(email)
                    || !(claims.get("roles") instanceof List<?> roles)
                    || roles.isEmpty()
                    || roles.stream().anyMatch(role -> !(role instanceof String value)
                    || !IDENTITY_ROLES.contains(value))) {
                return;
            }
            UUID userId = UUID.fromString(claims.getSubject());
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(String.class::cast)
                    .map(SimpleGrantedAuthority::new)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_MERCHANT"))) {
                MERCHANT_JWT_SCOPES.stream()
                        .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                        .forEach(authorities::add);
            }
            MerchantPrincipal principal = new MerchantPrincipal(userId,
                    claims.get("email", String.class), false);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, authorities));
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateApiKey(String presentedKey) {
        if (!hasValidApiKeyFormat(presentedKey)) {
            return;
        }
        String prefix = presentedKey.substring(0, API_KEY_PREFIX_LENGTH);
        MerchantApiKey apiKey = apiKeyRepository.findByKeyPrefixAndRevokedAtIsNull(prefix).orElse(null);
        if (apiKey == null || !constantTimeEquals(apiKey.getKeyHash(), sha256(presentedKey))) {
            return;
        }
        var profile = profileRepository.findById(apiKey.getMerchantId()).orElse(null);
        if (profile == null || profile.getApprovalStatus() != MerchantApprovalStatus.APPROVED) {
            return;
        }
        List<String> scopes = Arrays.asList(apiKey.getScopes().split(",", -1));
        if (scopes.isEmpty() || scopes.stream().anyMatch(scope -> !API_KEY_SCOPES.contains(scope))) {
            return;
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_MERCHANT"));
        scopes.stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .forEach(authorities::add);
        MerchantPrincipal principal = new MerchantPrincipal(profile.getUserId(), profile.getEmail(), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private boolean hasValidApiKeyFormat(String apiKey) {
        if (apiKey.length() != API_KEY_LENGTH || !apiKey.startsWith(API_KEY_PREFIX)) {
            return false;
        }
        return apiKey.substring(API_KEY_PREFIX.length()).chars()
                .allMatch(character -> character >= 'A' && character <= 'Z'
                        || character >= 'a' && character <= 'z'
                        || character >= '0' && character <= '9'
                        || character == '-' || character == '_');
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
