package com.workforce.upload.tenant;

/**
 * Thin wrapper over the shared {@link com.workforce.common.tenant.TenantContext}.
 * Preserves existing call sites without requiring import changes across the service.
 */
public final class TenantContext {
    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        com.workforce.common.tenant.TenantContext.setTenantId(tenantId);
    }

    public static String getTenantId() {
        return com.workforce.common.tenant.TenantContext.getTenantId();
    }

    public static void clear() {
        com.workforce.common.tenant.TenantContext.clear();
    }
}
