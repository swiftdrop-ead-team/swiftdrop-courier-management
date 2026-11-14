package com.swiftdrop.notification.security;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
 @Bean SecurityFilterChain apiSecurity(HttpSecurity http, JwtAuthenticationFilter jwt) throws Exception {
  return http.csrf(AbstractHttpConfigurer::disable)
   .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a -> a.requestMatchers("/actuator/health", "/actuator/info").permitAll().anyRequest().authenticated())
   .exceptionHandling(e -> e.authenticationEntryPoint((q,r,x) -> {r.setStatus(401);r.setContentType("application/json");r.getWriter().write("{\"message\":\"Authentication required\"}");})
    .accessDeniedHandler((q,r,x) -> {r.setStatus(403);r.setContentType("application/json");r.getWriter().write("{\"message\":\"Access denied\"}");}))
   .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
 }
}
