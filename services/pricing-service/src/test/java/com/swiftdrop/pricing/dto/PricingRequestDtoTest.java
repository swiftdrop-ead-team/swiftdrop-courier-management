package com.swiftdrop.pricing.dto;

import com.swiftdrop.pricing.entity.ServiceType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PricingRequestDto} Bean Validation constraints.
 * No Spring context is required – we use the standard jakarta.validation API.
 */
class PricingRequestDtoTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    private PricingRequestDto validDto() {
        return PricingRequestDto.builder()
                .serviceType(ServiceType.STANDARD)
                .originZone("ZONE_A")
                .destinationZone("ZONE_B")
                .physicalWeightKg(new BigDecimal("1.5"))
                .lengthCm(new BigDecimal("30"))
                .widthCm(new BigDecimal("20"))
                .heightCm(new BigDecimal("10"))
                .distanceKm(new BigDecimal("100"))
                .promoCode("PROMO10")
                .orderReference("ORDER123")
                .customerId("CUST001")
                .build();
    }

    @Test
    @DisplayName("Valid DTO produces no constraint violations")
    void validDtoHasNoViolations() {
        PricingRequestDto dto = validDto();
        Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Expected no violations for a valid DTO");
    }

    @Nested
    @DisplayName("Missing required fields")
    class MissingFields {
        @Test
        void missingServiceType() {
            PricingRequestDto dto = validDto();
            dto.setServiceType(null);
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("serviceType")));
        }

        @Test
        void missingOriginZone() {
            PricingRequestDto dto = validDto();
            dto.setOriginZone(null);
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("originZone")));
        }

        @Test
        void missingDestinationZone() {
            PricingRequestDto dto = validDto();
            dto.setDestinationZone(""); // blank string triggers @NotBlank
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("destinationZone")));
        }
    }

    @Nested
    @DisplayName("Physical weight constraints")
    class PhysicalWeight {
        @Test
        void zeroPhysicalWeight() {
            PricingRequestDto dto = validDto();
            dto.setPhysicalWeightKg(BigDecimal.ZERO);
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("physicalWeightKg")));
        }

        @Test
        void negativePhysicalWeight() {
            PricingRequestDto dto = validDto();
            dto.setPhysicalWeightKg(new BigDecimal("-1"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("physicalWeightKg")));
        }
    }

    @Nested
    @DisplayName("Dimension constraints (length, width, height)")
    class Dimensions {
        @Test
        void zeroLength() {
            PricingRequestDto dto = validDto();
            dto.setLengthCm(BigDecimal.ZERO);
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("lengthCm")));
        }

        @Test
        void negativeWidth() {
            PricingRequestDto dto = validDto();
            dto.setWidthCm(new BigDecimal("-5"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("widthCm")));
        }

        @Test
        void negativeHeight() {
            PricingRequestDto dto = validDto();
            dto.setHeightCm(new BigDecimal("-2"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("heightCm")));
        }
    }

    @Nested
    @DisplayName("Distance constraints")
    class Distance {
        @Test
        void zeroDistanceIsAllowed() {
            PricingRequestDto dto = validDto();
            dto.setDistanceKm(BigDecimal.ZERO);
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty(), "Zero distance should be allowed");
        }

        @Test
        void negativeDistance() {
            PricingRequestDto dto = validDto();
            dto.setDistanceKm(new BigDecimal("-10"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("distanceKm")));
        }
    }

    @Nested
    @DisplayName("Optional promo fields")
    class OptionalPromo {
        @Test
        void nullPromoFieldsAreValid() {
            PricingRequestDto dto = validDto();
            dto.setPromoCode(null);
            dto.setOrderReference(null);
            dto.setCustomerId(null);
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty(), "Optional promo fields may be null");
        }
    }
}
