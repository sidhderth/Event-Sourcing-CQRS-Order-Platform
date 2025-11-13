package com.orderplatform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter that generates or propagates X-Correlation-ID header
 * for distributed tracing across services
 */
@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Get or generate correlation ID
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Log the correlation ID
        log.debug("Processing request with correlation ID: {} for path: {}", 
            correlationId, request.getPath());

        // Extract user context from JWT (if authenticated)
        String userId = extractUserId(exchange);
        String userRoles = extractUserRoles(exchange);

        // Add correlation ID and user context to downstream requests
        ServerHttpRequest mutatedRequest = request.mutate()
            .header(CORRELATION_ID_HEADER, correlationId)
            .header(USER_ID_HEADER, userId != null ? userId : "")
            .header(USER_ROLES_HEADER, userRoles != null ? userRoles : "")
            .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build();

        return chain.filter(mutatedExchange);
    }

    private String extractUserId(ServerWebExchange exchange) {
        return exchange.getPrincipal()
            .map(principal -> principal.getName())
            .block();
    }

    private String extractUserRoles(ServerWebExchange exchange) {
        return exchange.getPrincipal()
            .flatMap(principal -> {
                if (principal instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) {
                    org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtToken = 
                        (org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) principal;
                    
                    String roles = jwtToken.getAuthorities().stream()
                        .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                    
                    return Mono.just(roles);
                }
                return Mono.empty();
            })
            .block();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
