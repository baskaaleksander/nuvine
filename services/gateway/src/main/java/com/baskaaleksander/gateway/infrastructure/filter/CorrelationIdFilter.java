package com.baskaaleksander.gateway.infrastructure.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(1)
public class CorrelationIdFilter implements GlobalFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String finalCorrelationId = correlationId;
        return chain.filter(
                exchange.mutate()
                        .request(builder -> builder.header(CORRELATION_ID_HEADER, finalCorrelationId))
                        .build()
        ).contextWrite(context -> {
            MDC.put(CORRELATION_ID_HEADER, finalCorrelationId);
            return context;
        });
    }
}
