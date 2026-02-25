package com.workforce.common.tenant;

/**
 * Request-scoped tenant context.
 * Populated by JwtAuthenticationFilter from either gateway headers or JWT claims.
 */
public final class TenantContext {
    private static final InheritableThreadLocal<String> TENANT = new InheritableThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) { TENANT.set(tenantId); }
    public static String getTenantId()               { return TENANT.get(); }
    public static void clear()                       { TENANT.remove(); }
}
