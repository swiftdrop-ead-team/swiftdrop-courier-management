package com.swiftdrop.dispatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Dispatch Microservice.
 * This service manages couriers, workload balancing, parcel assignments,
 * and proof-of-delivery records for SwiftDrop.
 */
@SpringBootApplication
public class DispatchServiceApplication {

    /**
     * Starts the Spring Boot application on port 8084.
     * @param args Command line arguments passed during application startup.
     */
    public static void main(String[] args) {
        SpringApplication.run(DispatchServiceApplication.class, args);
    }
}
