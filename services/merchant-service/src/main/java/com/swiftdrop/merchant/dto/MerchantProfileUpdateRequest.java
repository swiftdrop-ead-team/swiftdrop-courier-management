package com.swiftdrop.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantProfileUpdateRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 150, message = "Business name must not exceed 150 characters")
    private String businessName;

    @NotBlank(message = "Contact person is required")
    @Size(max = 150, message = "Contact person must not exceed 150 characters")
    private String contactPerson;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+0-9() .-]{7,30}$", message = "Phone number is invalid")
    private String phoneNumber;

    @NotBlank(message = "Business address is required")
    @Size(max = 2000, message = "Business address must not exceed 2000 characters")
    private String businessAddress;
}
