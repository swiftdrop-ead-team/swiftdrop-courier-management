package com.swiftdrop.parcel.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TrackingNumberGenerator {

    private static final String DIGITS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a tracking number adhering to the SwiftDrop standard format:
     * SD-XXXX-XXXX-XX (e.g., SD-4829-1094-US or SD-9021-4820-LK)
     */
    public String generate() {
        return generate("LK");
    }

    public String generate(String countryCode) {
        String cc = (countryCode != null && countryCode.trim().length() == 2)
                ? countryCode.trim().toUpperCase()
                : "LK";

        StringBuilder sb = new StringBuilder("SD-");

        // First 4 digits
        for (int i = 0; i < 4; i++) {
            sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        sb.append("-");

        // Second 4 digits
        for (int i = 0; i < 4; i++) {
            sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        sb.append("-");

        // Country code / Region suffix (2 characters)
        sb.append(cc);

        return sb.toString();
    }
}

