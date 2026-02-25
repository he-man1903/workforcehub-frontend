package com.workforce.gateway.filter;

import com.workforce.gateway.security.GatewayHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TenantPropagationFilter extends AbstractGatewayFilterFactory<TenantPropagationFilter.Config> {

    public TenantPropagationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String tenantId = exchange.getRequest().getHeaders().getFirst(GatewayHeaders.X_TENANT_ID);

            if (tenantId == null || tenantId.isBlank()) {
                log.error("TenantPropagationFilter: X-Tenant-Id header is missing — this should have been set by JwtAuthenticationFilter");
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return exchange.getResponse().setComplete();
            }

            log.debug("Propagating tenantId={} downstream", tenantId);
            return chain.filter(exchange);
        };
    }

    public static class Config {}
}
