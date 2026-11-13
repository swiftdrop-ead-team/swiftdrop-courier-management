package com.swiftdrop.pricing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.pricing.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for SwiftDrop Pricing Service.
 * Configures stateless JWT authentication, public vs protected endpoints,
 * and standardized error handling for unauthorized (401) and forbidden (403) access.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            ApiResponse<Void> apiResponse = ApiResponse.error("Full authentication is required to access this resource");
                            objectMapper.writeValue(response.getOutputStream(), apiResponse);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            ApiResponse<Void> apiResponse = ApiResponse.error("You do not have permission to access this resource");
                            objectMapper.writeValue(response.getOutputStream(), apiResponse);
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Public health and diagnostic endpoints
                        .requestMatchers(
                                "/api/v1/pricing/health",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        // Public API documentation
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Preflight CORS requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Protected pricing quote endpoints - allowed for CUSTOMER, MERCHANT, and ADMIN roles
                        .requestMatchers("/api/v1/pricing/quotes/**")
                        .hasAnyRole("CUSTOMER", "MERCHANT", "ADMIN")
                        // Any other request requires authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
