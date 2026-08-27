package com.platform.ai.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class, DataIntegrityViolationException.class})
    ResponseEntity<Map<String, Object>> badRequest(Exception e) { return error(HttpStatus.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> upstream(Exception e) { return error(HttpStatus.BAD_GATEWAY, e.getMessage()); }
    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now().toString(), "status", status.value(), "message", message == null ? status.getReasonPhrase() : message));
    }
}
