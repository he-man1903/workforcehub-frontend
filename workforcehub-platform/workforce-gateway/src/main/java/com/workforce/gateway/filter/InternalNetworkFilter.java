package com.workforce.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Set;

@Slf4j
@Component
public class InternalNetworkFilter extends AbstractGatewayFilterFactory<InternalNetworkFilter.Config> {

    private static final Set<String> INTERNAL_PREFIXES = Set.of("10.", "172.16.", "172.17.", "192.168.", "127.");

    public InternalNetworkFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress == null) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            String ip = remoteAddress.getAddress().getHostAddress();
            boolean isInternal = INTERNAL_PREFIXES.stream().anyMatch(ip::startsWith);

            if (!isInternal) {
                log.warn("Blocked external access to internal route from ip={}", ip);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }

    public static class Config {}
}
