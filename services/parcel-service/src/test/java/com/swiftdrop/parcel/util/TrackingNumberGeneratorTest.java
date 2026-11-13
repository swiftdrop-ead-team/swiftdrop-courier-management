package com.swiftdrop.parcel.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class TrackingNumberGeneratorTest {

    private TrackingNumberGenerator generator;
    private static final Pattern TRACKING_PATTERN = Pattern.compile("^SD-\\d{4}-\\d{4}-[A-Z]{2}$");

    @BeforeEach
    void setUp() {
        generator = new TrackingNumberGenerator();
    }

    @Test
    @DisplayName("Should generate valid default tracking number matching SD-XXXX-XXXX-LK format")
    void shouldGenerateValidDefaultTrackingNumber() {
        String trackingNumber = generator.generate();

        assertNotNull(trackingNumber);
        assertTrue(TRACKING_PATTERN.matcher(trackingNumber).matches(),
                "Tracking number must match SD-XXXX-XXXX-XX pattern: " + trackingNumber);
        assertTrue(trackingNumber.endsWith("-LK"));
    }

    @Test
    @DisplayName("Should generate valid tracking number with custom country code")
    void shouldGenerateWithCustomCountryCode() {
        String trackingNumber = generator.generate("US");

        assertNotNull(trackingNumber);
        assertTrue(TRACKING_PATTERN.matcher(trackingNumber).matches());
        assertTrue(trackingNumber.endsWith("-US"));
    }

    @Test
    @DisplayName("Should fallback to LK when invalid country code is provided")
    void shouldFallbackWhenInvalidCountryCode() {
        String trackingNumber = generator.generate("TOOLONG");
        assertTrue(trackingNumber.endsWith("-LK"));

        String nullCodeNumber = generator.generate(null);
        assertTrue(nullCodeNumber.endsWith("-LK"));
    }

    @Test
    @DisplayName("Should generate unique tracking numbers across 1,000 invocations")
    void shouldGenerateUniqueNumbers() {
        Set<String> generatedNumbers = new HashSet<>();
        int count = 1000;

        for (int i = 0; i < count; i++) {
            String trackingNumber = generator.generate();
            assertTrue(generatedNumbers.add(trackingNumber),
                    "Collision detected on tracking number: " + trackingNumber);
        }

        assertEquals(count, generatedNumbers.size());
    }
}

