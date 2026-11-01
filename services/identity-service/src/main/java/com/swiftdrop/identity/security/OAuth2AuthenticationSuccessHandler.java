package com.swiftdrop.identity.security;

import com.swiftdrop.identity.entity.*;
import com.swiftdrop.identity.repository.OAuthIdentityRepository;
import com.swiftdrop.identity.repository.RoleRepository;
import com.swiftdrop.identity.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OAuthIdentityRepository oauthIdentityRepository;
    private final CookieUtils cookieUtils;

    @Value("${swiftdrop.security.oauth2-redirect-url:http://localhost:3000/auth/callback}")
    private String redirectUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = authToken.getPrincipal();
        String registrationId = authToken.getAuthorizedClientRegistrationId();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerUserId = oAuth2User.getAttribute("sub");

        if (email == null) {
            log.error("OAuth2 user does not provide an email address");
            response.sendRedirect(redirectUrl + "?error=email_not_provided");
            return;
        }

        // 1. Find existing user or register new customer
        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(email);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
        } else {
            Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                    .orElseThrow(() -> new IllegalStateException("ROLE_CUSTOMER not configured"));

            user = User.builder()
                    .email(email.toLowerCase().trim())
                    .fullName(name != null ? name : "Google User")
                    .status(UserStatus.ACTIVE)
                    .roles(Collections.singleton(customerRole))
                    .build();
            user = userRepository.save(user);
        }

        // 2. Link OAuth identity if not present
        if (providerUserId != null && oauthIdentityRepository.findByUserIdAndProvider(user.getId(), registrationId).isEmpty()) {
            OAuthIdentity identity = OAuthIdentity.builder()
                    .user(user)
                    .provider(registrationId)
                    .providerUserId(providerUserId)
                    .providerEmail(email)
                    .build();
            oauthIdentityRepository.save(identity);
        }

        // 3. Generate JWT and refresh token
        String jwtToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();
        cookieUtils.addRefreshTokenCookie(response, refreshToken, jwtService.getRefreshExpirationMs());

        // 4. Redirect to frontend with token
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("token", jwtToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
