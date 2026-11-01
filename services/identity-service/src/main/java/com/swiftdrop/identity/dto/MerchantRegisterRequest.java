package com.swiftdrop.identity.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRegisterRequest {

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 200, message = "Business name must be between 2 and 200 characters")
    private String businessName;

    @NotBlank(message = "Contact person is required")
    @Size(min = 2, max = 150, message = "Contact person must be between 2 and 150 characters")
    private String contactPerson;

    @NotBlank(message = "Business email is required")
    @Email(message = "Must be a valid business email address")
    private String email;

    @NotBlank(message = "Business address is required")
    @Size(min = 5, max = 500, message = "Business address must be between 5 and 500 characters")
    private String businessAddress;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must be valid digits between 7 and 15 characters")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
