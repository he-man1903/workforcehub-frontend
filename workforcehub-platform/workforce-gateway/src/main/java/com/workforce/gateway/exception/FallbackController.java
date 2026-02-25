package com.workforce.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFallback() {
        log.warn("Upload service circuit breaker triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "Service Unavailable",
                        "message", "Upload service is temporarily unavailable. Please try again later.",
                        "timestamp", Instant.now().toString()
                ));
    }

    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> queryFallback() {
        log.warn("Query service circuit breaker triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "Service Unavailable",
                        "message", "Query service is temporarily unavailable. Please try again later.",
                        "timestamp", Instant.now().toString()
                ));
    }
}
