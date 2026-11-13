package com.swiftdrop.merchant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI merchantServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftDrop Merchant Service API")
                        .description("Merchant profile, order, API key, and webhook management (Member 4)")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Direct Merchant Service"),
                        new Server().url("http://localhost:8080").description("Via API Gateway")));
    }
}
