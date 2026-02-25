package com.workforce.auth.exception;

import com.workforce.auth.dto.response.ErrorResponse;
import com.workforce.auth.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<ErrorResponse> handleRateLimit(
                        RateLimitExceededException ex, HttpServletRequest req) {
                log.warn("Rate limit exceeded: {}", req.getRemoteAddr());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .body(ErrorResponse.of(
                                                HttpStatus.TOO_MANY_REQUESTS.value(),
                                                "Too Many Requests",
                                                ex.getMessage()));
        }

        @ExceptionHandler(InvalidGoogleTokenException.class)
        public ResponseEntity<ErrorResponse> handleInvalidGoogle(InvalidGoogleTokenException ex) {
                log.warn("Invalid Google token: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorResponse.of(401, "Unauthorized", ex.getMessage()));
        }

        @ExceptionHandler(InvalidRefreshTokenException.class)
        public ResponseEntity<ErrorResponse> handleInvalidRefresh(InvalidRefreshTokenException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorResponse.of(401, "Unauthorized", ex.getMessage()));
        }

        @ExceptionHandler(UserDisabledException.class)
        public ResponseEntity<ErrorResponse> handleDisabled(UserDisabledException ex) {
                log.warn("Disabled user attempted login: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
                Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                                .collect(Collectors.toMap(FieldError::getField,
                                                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage()
                                                                : "Invalid value"));
                return ResponseEntity.badRequest()
                                .body(ErrorResponse.withFields(400, "Bad Request", "Validation failed", fieldErrors));
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ErrorResponse> handleConfig(IllegalStateException ex) {
                log.warn("Configuration error: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(ErrorResponse.of(503, "Service Unavailable", ex.getMessage()));
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex, HttpServletRequest req) {
                log.warn("No handler for path {} — is the auth service image up to date? Rebuild with: docker compose build --no-cache workforce-auth-service",
                                req.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ErrorResponse.of(404, "Not Found",
                                                "No endpoint for this path. Rebuild the auth service image and restart: docker compose build --no-cache workforce-auth-service && docker compose up -d",
                                                ex.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
                log.error("Unhandled exception at {}", req.getRequestURI(), ex);
                String detail = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.of(500, "Internal Server Error",
                                                "An unexpected error occurred", detail));
        }
}
