package com.swiftdrop.pricing.exception;

import com.swiftdrop.pricing.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("QuoteNotFoundException maps to 404 Not Found")
    void testHandleQuoteNotFound() {
        UUID id = UUID.randomUUID();
        QuoteNotFoundException ex = new QuoteNotFoundException(id);

        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains(id.toString()));
    }

    @Test
    @DisplayName("ZoneNotFoundException maps to 404 Not Found")
    void testHandleZoneNotFound() {
        ZoneNotFoundException ex = new ZoneNotFoundException("Zone not found: ZONE_XYZ");

        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Zone not found: ZONE_XYZ", response.getBody().getMessage());
    }

    @Test
    @DisplayName("QuoteExpiredException maps to 410 Gone")
    void testHandleQuoteExpired() {
        UUID id = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.of(2026, 11, 10, 12, 0, 0);
        QuoteExpiredException ex = new QuoteExpiredException(id, expiresAt);

        ResponseEntity<ApiResponse<Void>> response = handler.handleQuoteExpired(ex);

        assertEquals(HttpStatus.GONE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("expired"));
    }

    @Test
    @DisplayName("InactiveZoneException maps to 400 Bad Request")
    void testHandleInactiveZone() {
        InactiveZoneException ex = new InactiveZoneException("Zone is inactive: ZONE_A");

        ResponseEntity<ApiResponse<Void>> response = handler.handleClientBusinessError(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Zone is inactive: ZONE_A", response.getBody().getMessage());
    }

    @Test
    @DisplayName("InvalidPromoCodeException maps to 400 Bad Request")
    void testHandleInvalidPromoCode() {
        InvalidPromoCodeException ex = new InvalidPromoCodeException("Promo code expired: PROMO10");

        ResponseEntity<ApiResponse<Void>> response = handler.handleClientBusinessError(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Promo code expired: PROMO10", response.getBody().getMessage());
    }

    @Test
    @DisplayName("DeliveryRestrictionException maps to 400 Bad Request")
    void testHandleDeliveryRestriction() {
        DeliveryRestrictionException ex = new DeliveryRestrictionException("Package exceeds weight limit");

        ResponseEntity<ApiResponse<Void>> response = handler.handleClientBusinessError(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Package exceeds weight limit", response.getBody().getMessage());
    }

    @Test
    @DisplayName("UnsupportedPricingTierException maps to 400 Bad Request")
    void testHandleUnsupportedTier() {
        UnsupportedPricingTierException ex = new UnsupportedPricingTierException("Unsupported tier");

        ResponseEntity<ApiResponse<Void>> response = handler.handleClientBusinessError(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Unsupported tier", response.getBody().getMessage());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException maps to 400 Bad Request with field errors")
    void testHandleValidationException() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "pricingRequestDto");
        bindingResult.addError(new FieldError("pricingRequestDto", "physicalWeightKg", "Physical weight must be greater than zero"));
        bindingResult.addError(new FieldError("pricingRequestDto", "originZone", "Origin zone is required"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
        assertEquals("Physical weight must be greater than zero", response.getBody().getData().get("physicalWeightKg"));
        assertEquals("Origin zone is required", response.getBody().getData().get("originZone"));
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException maps to 400 Bad Request")
    void testHandleTypeMismatch() throws NoSuchMethodException {
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleMethod", UUID.class), 0
        );
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "invalid-uuid", UUID.class, "id", parameter, null
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Invalid value 'invalid-uuid' for parameter 'id'"));
    }

    @Test
    @DisplayName("HttpMessageNotReadableException maps to 400 Bad Request")
    void testHandleMessageNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Malformed request payload or unreadable JSON format", response.getBody().getMessage());
    }

    @Test
    @DisplayName("FuelRateNotFoundException maps to 500 with safe message")
    void testHandleFuelRateNotFound() {
        FuelRateNotFoundException ex = new FuelRateNotFoundException("No active rate for 2026-11-14");

        ResponseEntity<ApiResponse<Void>> response = handler.handleFuelRateNotFound(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Current fuel surcharge rate is not configured. Please contact support.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Unhandled Exception maps to 500 with generic safe message")
    void testHandleGeneralException() {
        NullPointerException ex = new NullPointerException("Secret internal database connection string leaked");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("An unexpected internal error occurred. Please try again later.", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("Secret"));
    }

    // Helper method used for MethodParameter reflection in testHandleTypeMismatch
    private void sampleMethod(UUID id) {
    }
}
