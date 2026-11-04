package com.swiftdrop.dispatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger / OpenAPI 3 configuration.
 * Generates interactive API documentation accessible via browser at:
 * http://localhost:8084/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    // Reads server port from application.yml (default is 8084)
    @Value("${server.port:8084}")
    private String serverPort;

    /**
     * Creates and configures the OpenAPI metadata object.
     * Displays title, description, version, and server URLs in Swagger UI.
     */
    @Bean
    public OpenAPI dispatchServiceOpenAPI() {
        return new OpenAPI()
                // Set metadata information for the Swagger documentation page
                .info(new Info()
                        .title("SwiftDrop — Dispatch & Workload Management API")
                        .description("Microservice responsible for courier workload balancing, route assignments, and proof of delivery verification (Member 5)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SwiftDrop Logistics Engineering Team")
                                .email("dev@swiftdrop.lk"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                // Provide direct server URL and Gateway URL for testing API calls
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Direct Dispatch Service"),
                        new Server().url("http://localhost:8080").description("Via API Gateway")
                ));
    }
}
