package com.swiftdrop.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookSubscriptionRequest {

    @NotBlank(message = "Webhook URL is required")
    @Size(max = 2048, message = "Webhook URL must not exceed 2048 characters")
    private String targetUrl;

    @NotEmpty(message = "At least one event type is required")
    @Size(max = 10, message = "No more than ten event types are allowed")
    private Set<@NotBlank @Size(max = 100) String> eventTypes;
}
