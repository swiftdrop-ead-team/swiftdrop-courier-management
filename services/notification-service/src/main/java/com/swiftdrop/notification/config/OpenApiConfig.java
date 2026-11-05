package com.swiftdrop.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger OpenAPI documentation configuration for Notification Service (Member 8).
 * Accessible at: http://localhost:8087/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftDrop — Notification Service API")
                        .description("Multi-channel asynchronous notification dispatch microservice. Consumes domain events from RabbitMQ and delivers email alerts with idempotency protection and delivery logs.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SwiftDrop EAD Engineering Team — Member 8")
                                .email("notification-team@swiftdrop.io"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}

