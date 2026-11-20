package com.swiftdrop.gateway.filter;

import com.swiftdrop.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            // These exact public summaries are sanitized by Parcel Service.
            if (exchange.getRequest().getMethod() == org.springframework.http.HttpMethod.GET
                    && path.matches("/api/v1/parcels/tracking/[A-Za-z0-9-]{4,50}"))
                return chain.filter(exchange);
            // Merchant Service authenticates scoped, hashed integration keys itself.
            if (path.startsWith("/api/v1/merchant/")
                    && !exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)
                    && org.springframework.util.StringUtils.hasText(exchange.getRequest().getHeaders().getFirst("X-API-Key")))
                return chain.filter(exchange);
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authHeader = authHeader.substring(7);
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            try {
                jwtUtil.validateToken(authHeader);
            } catch (Exception e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Configuration properties can go here if needed
    }
}