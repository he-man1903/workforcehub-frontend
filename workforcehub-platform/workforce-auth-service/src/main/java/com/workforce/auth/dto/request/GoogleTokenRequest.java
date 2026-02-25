package com.workforce.auth.dto.request;
import jakarta.validation.constraints.NotBlank;
public record GoogleTokenRequest(@NotBlank(message = "idToken must not be blank") String idToken) {}
