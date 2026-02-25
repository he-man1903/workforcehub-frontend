package com.workforce.query.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_ID_HEADER      = "X-Tenant-Id";
    public static final String USER_ID_HEADER        = "X-User-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String tenantId = request.getHeader(TENANT_ID_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Direct access rejected — missing X-Tenant-Id");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("{\"status\":403,\"message\":\"Direct service access is not allowed\"}");
            return false;
        }
        TenantContext.setTenantId(tenantId);
        MDC.put("tenantId", tenantId);
        String userId = request.getHeader(USER_ID_HEADER);
        String corrId = request.getHeader(CORRELATION_ID_HEADER);
        if (userId != null) MDC.put("userId", userId);
        if (corrId != null) MDC.put("correlationId", corrId);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        TenantContext.clear();
        MDC.remove("tenantId");
        MDC.remove("userId");
        MDC.remove("correlationId");
    }
}
