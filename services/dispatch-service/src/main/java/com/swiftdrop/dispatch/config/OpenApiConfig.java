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

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8084}")
    private String serverPort;

    @Bean
    public OpenAPI dispatchServiceOpenAPI() {
        return new OpenAPI()
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
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Direct Dispatch Service"),
                        new Server().url("http://localhost:8080").description("Via API Gateway")
                ));
    }
}

