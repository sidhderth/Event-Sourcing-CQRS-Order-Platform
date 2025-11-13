package com.orderplatform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that adds security headers to all responses
 * Protects against common web vulnerabilities
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            
            // Prevent MIME type sniffing
            headers.add("X-Content-Type-Options", "nosniff");
            
            // Prevent clickjacking attacks
            headers.add("X-Frame-Options", "DENY");
            
            // Enable HSTS (HTTP Strict Transport Security)
            // max-age=31536000 (1 year), includeSubDomains
            headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            
            // Prevent XSS attacks
            headers.add("X-XSS-Protection", "1; mode=block");
            
            // Control referrer information
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // Content Security Policy (basic policy)
            headers.add("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'");
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
