package com.swiftdrop.tracking.controller;

import com.swiftdrop.tracking.service.TrackingNotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class TrackingExceptionHandler {
    @ExceptionHandler(TrackingNotFoundException.class)
    ResponseEntity<Map<String, Object>> notFound(TrackingNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", 404, "error", "Not Found", "message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("status", 400, "error", "Bad Request", "message", exception.getMessage()));
    }
}
