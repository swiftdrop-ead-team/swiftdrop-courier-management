package com.swiftdrop.pricing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger / OpenAPI 3 configuration for the Pricing Microservice.
 * Generates interactive API documentation accessible via browser at:
 * http://localhost:8085/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8085}")
    private String serverPort;

    @Bean
    public OpenAPI pricingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftDrop — Dynamic Pricing & Billing API")
                        .description("Microservice responsible for real-time shipping rate calculation, urgency tiers, merchant discounts, promo codes, and fuel surcharge indexing (Member 6)")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Direct Pricing Service"),
                        new Server().url("http://localhost:8080").description("Via API Gateway")
                ));
    }
}
