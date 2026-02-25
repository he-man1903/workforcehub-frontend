package com.workforce.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors,
        String detail) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, Instant.now(), null, null);
    }

    public static ErrorResponse of(int status, String error, String message, String detail) {
        return new ErrorResponse(status, error, message, Instant.now(), null, detail);
    }

    public static ErrorResponse withFields(int status, String error, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(status, error, message, Instant.now(), fieldErrors, null);
    }
}
