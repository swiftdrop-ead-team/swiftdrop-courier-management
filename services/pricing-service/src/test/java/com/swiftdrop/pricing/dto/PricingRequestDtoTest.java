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
 * No Spring context is required – uses the standard jakarta.validation API.
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
                .physicalWeightKg(new BigDecimal("1.50"))
                .lengthCm(new BigDecimal("30.00"))
                .widthCm(new BigDecimal("20.00"))
                .heightCm(new BigDecimal("10.00"))
                .distanceKm(new BigDecimal("100.00"))
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
    @DisplayName("String length constraints")
    class StringLengths {
        @Test
        @DisplayName("Origin zone exceeding 50 characters is rejected")
        void originZoneTooLong() {
            PricingRequestDto dto = validDto();
            dto.setOriginZone("A".repeat(51));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("originZone")));
        }

        @Test
        @DisplayName("Destination zone exceeding 50 characters is rejected")
        void destinationZoneTooLong() {
            PricingRequestDto dto = validDto();
            dto.setDestinationZone("B".repeat(51));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("destinationZone")));
        }

        @Test
        @DisplayName("Promo code exceeding 50 characters is rejected")
        void promoCodeTooLong() {
            PricingRequestDto dto = validDto();
            dto.setPromoCode("P".repeat(51));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("promoCode")));
        }

        @Test
        @DisplayName("Order reference exceeding 100 characters is rejected")
        void orderReferenceTooLong() {
            PricingRequestDto dto = validDto();
            dto.setOrderReference("O".repeat(101));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("orderReference")));
        }

        @Test
        @DisplayName("Customer ID exceeding 100 characters is rejected")
        void customerIdTooLong() {
            PricingRequestDto dto = validDto();
            dto.setCustomerId("C".repeat(101));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("customerId")));
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
            dto.setPhysicalWeightKg(new BigDecimal("-1.00"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("physicalWeightKg")));
        }

        @Test
        @DisplayName("Maximum boundary value 999999.99 is accepted")
        void maxPhysicalWeightAccepted() {
            PricingRequestDto dto = validDto();
            dto.setPhysicalWeightKg(new BigDecimal("999999.99"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty(), "999999.99 kg must be valid");
        }

        @Test
        @DisplayName("Physical weight 1000000 is rejected")
        void exceedingMaxPhysicalWeightRejected() {
            PricingRequestDto dto = validDto();
            dto.setPhysicalWeightKg(new BigDecimal("1000000.00"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("physicalWeightKg")));
        }

        @Test
        @DisplayName("More than 2 decimal places is rejected")
        void excessiveDecimalsRejected() {
            PricingRequestDto dto = validDto();
            dto.setPhysicalWeightKg(new BigDecimal("1.234"));
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
            dto.setWidthCm(new BigDecimal("-5.00"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("widthCm")));
        }

        @Test
        void negativeHeight() {
            PricingRequestDto dto = validDto();
            dto.setHeightCm(new BigDecimal("-2.00"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("heightCm")));
        }

        @Test
        @DisplayName("Dimension at maximum boundary 999999.99 is accepted")
        void maxDimensionAccepted() {
            PricingRequestDto dto = validDto();
            dto.setLengthCm(new BigDecimal("999999.99"));
            dto.setWidthCm(new BigDecimal("999999.99"));
            dto.setHeightCm(new BigDecimal("999999.99"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty(), "999999.99 cm must be valid");
        }

        @Test
        @DisplayName("Dimension 1000000 is rejected")
        void exceedingMaxDimensionRejected() {
            PricingRequestDto dto = validDto();
            dto.setLengthCm(new BigDecimal("1000000.00"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("lengthCm")));
        }

        @Test
        @DisplayName("Dimension with more than 2 decimal places is rejected")
        void excessiveDimensionDecimalsRejected() {
            PricingRequestDto dto = validDto();
            dto.setWidthCm(new BigDecimal("25.125"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("widthCm")));
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
            dto.setDistanceKm(new BigDecimal("-10.00"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("distanceKm")));
        }

        @Test
        @DisplayName("Distance at maximum boundary 999999.99 is accepted")
        void maxDistanceAccepted() {
            PricingRequestDto dto = validDto();
            dto.setDistanceKm(new BigDecimal("999999.99"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty(), "999999.99 km must be valid");
        }

        @Test
        @DisplayName("Distance 1000000 is rejected")
        void exceedingMaxDistanceRejected() {
            PricingRequestDto dto = validDto();
            dto.setDistanceKm(new BigDecimal("1000000.00"));
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("distanceKm")));
        }

        @Test
        @DisplayName("Distance with more than 2 decimal places is rejected")
        void excessiveDistanceDecimalsRejected() {
            PricingRequestDto dto = validDto();
            dto.setDistanceKm(new BigDecimal("15.555"));
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

        @Test
        void emptyPromoFieldsAreValid() {
            PricingRequestDto dto = validDto();
            dto.setPromoCode("");
            dto.setOrderReference("");
            dto.setCustomerId("");
            Set<ConstraintViolation<PricingRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty(), "Empty string promo fields are allowed by @Size");
        }
    }
}
