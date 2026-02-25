package com.workforce.gateway.security;

public final class GatewayHeaders {

    // Downstream internal headers (set by gateway, trusted by services)
    public static final String X_TENANT_ID = "X-Tenant-Id";
    public static final String X_USER_ID = "X-User-Id";
    public static final String X_USER_ROLE = "X-User-Role";
    public static final String X_USER_EMAIL = "X-User-Email";
    public static final String X_CORRELATION_ID = "X-Correlation-Id";
    public static final String X_GATEWAY_TOKEN = "X-Gateway-Token";

    // Inbound
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private GatewayHeaders() {
    }
}
