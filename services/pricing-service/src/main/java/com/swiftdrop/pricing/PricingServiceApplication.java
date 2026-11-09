package com.swiftdrop.pricing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SwiftDrop — Dynamic Pricing & Billing Microservice (Member 6)
 *
 * Provides real-time delivery rate calculations, urgency tier rating,
 * merchant tier discounts, promo code redemption, and administrative pricing rules.
 */
@SpringBootApplication
public class PricingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricingServiceApplication.class, args);
    }
}
