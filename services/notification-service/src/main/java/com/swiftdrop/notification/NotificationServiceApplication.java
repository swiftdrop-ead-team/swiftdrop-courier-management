package com.swiftdrop.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the SwiftDrop Notification Microservice (Member 8).
 * Handles asynchronous event consumption from RabbitMQ and multi-channel email alerts.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

