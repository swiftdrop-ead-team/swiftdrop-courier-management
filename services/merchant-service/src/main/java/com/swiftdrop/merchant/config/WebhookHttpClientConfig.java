package com.swiftdrop.merchant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class WebhookHttpClientConfig {

    @Bean
    HttpClient webhookHttpClient(@Value("${merchant.webhook.request-timeout-seconds:10}") long timeoutSeconds) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }
}
