package com.workforce.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long   expiresIn,
        String tokenType,
        String tenantId,
        String userId,
        String email,
        String name,
        String role,
        String avatarUrl
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn,
            String tenantId, String userId, String email, String name, String role, String avatarUrl) {
        return new AuthResponse(accessToken, refreshToken, expiresIn,
                "Bearer", tenantId, userId, email, name, role, avatarUrl);
    }
}
