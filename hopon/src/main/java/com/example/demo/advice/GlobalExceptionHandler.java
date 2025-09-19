package com.example.demo.advice;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("ok", false, "reason", "CONSTRAINT_VIOLATION", "message", "Duplicate or invalid data"));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> handleIllegalArg(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "BAD_REQUEST", "message", e.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<?> handleIllegalState(IllegalStateException e) {
    return ResponseEntity.status(409).body(Map.of("ok", false, "reason", "CONFLICT", "message", e.getMessage()));
  }
}
