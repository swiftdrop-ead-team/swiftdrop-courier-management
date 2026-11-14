package com.swiftdrop.notification.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import java.util.Map;
@RestControllerAdvice @Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiAccessExceptionHandler {
 @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,org.springframework.orm.ObjectOptimisticLockingFailureException.class})
 public ResponseEntity<Map<String,Object>> conflict(Exception e){return ResponseEntity.status(409).body(Map.of("success",false,"message","The record changed or conflicts with existing data. Refresh and try again."));}
 @ExceptionHandler(AccessDeniedException.class)
 public ResponseEntity<Map<String,Object>> denied(AccessDeniedException e){return ResponseEntity.status(403).body(Map.of("success",false,"message","Access denied"));}
 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<Map<String,Object>> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("success",false,"message",e.getReason()==null?"Request rejected":e.getReason()));}
}
