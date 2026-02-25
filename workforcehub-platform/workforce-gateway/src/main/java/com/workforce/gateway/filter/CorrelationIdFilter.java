package com.workforce.gateway.filter;

import com.workforce.gateway.security.GatewayHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter extends AbstractGatewayFilterFactory<CorrelationIdFilter.Config> {

    public CorrelationIdFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest().getHeaders().getFirst(GatewayHeaders.X_CORRELATION_ID);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            final String finalCorrelationId = correlationId;

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(GatewayHeaders.X_CORRELATION_ID, finalCorrelationId)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                    .doFinally(s -> log.debug("Request completed: correlationId={}", finalCorrelationId));
        };
    }

    public static class Config {}
}
