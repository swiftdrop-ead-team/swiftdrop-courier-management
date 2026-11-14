package com.swiftdrop.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(GatewayApplicationTests.MockRouteConfig.class)
class GatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
        // Verifies the Spring Application Context loads successfully
    }

    @Test
    void testSecurityHeadersAndCorrelationIdArePresent() {
        webTestClient.get()
                .uri("/mock-route") // Hits the mock gateway route defined below
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Correlation-Id")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY")
                .expectHeader().valueEquals("X-XSS-Protection", "1; mode=block")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin");
    }

    // --- Mock Configuration to simulate a downstream microservice ---
    @TestConfiguration
    static class MockRouteConfig {

        @Bean
        public RouteLocator testRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                    // Routes /mock-route through the Gateway filters, then to the local controller
                    .route("test_route", r -> r.path("/mock-route")
                            .uri("forward:/local-mock"))
                    .build();
        }

        @RestController
        static class MockController {
            @GetMapping("/local-mock")
            public String handle() {
                return "Mock Downstream Response";
            }
        }
    }
}