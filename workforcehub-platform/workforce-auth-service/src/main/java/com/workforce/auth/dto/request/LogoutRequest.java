package com.workforce.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "refreshToken must not be blank")
        String refreshToken
) {}
