package com.swiftdrop.merchant.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantApiKeyRequest {

    @NotEmpty(message = "At least one API key scope is required")
    @Size(max = 5, message = "No more than five API key scopes are allowed")
    private Set<@Pattern(regexp = "orders:read|orders:write|dashboard:read",
            message = "Unsupported API key scope") String> scopes;
}
