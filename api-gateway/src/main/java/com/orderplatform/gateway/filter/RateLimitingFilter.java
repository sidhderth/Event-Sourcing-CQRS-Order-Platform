package com.orderplatform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Global filter that implements rate limiting per client
 * Uses in-memory storage for local development
 */
@Slf4j
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    @Value("${rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    // In-memory rate limit storage: clientId -> RateLimitBucket
    private final Map<String, RateLimitBucket> rateLimitStore = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Extract client identifier (user ID or IP address)
        String clientId = extractClientId(exchange);

        // Get or create rate limit bucket for this client
        RateLimitBucket bucket = rateLimitStore.computeIfAbsent(
            clientId, 
            k -> new RateLimitBucket(requestsPerMinute)
        );

        // Check if request is allowed
        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for client: {}", clientId);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            exchange.getResponse().getHeaders().add("X-RateLimit-Reset", 
                String.valueOf(bucket.getResetTime().getEpochSecond()));
            return exchange.getResponse().setComplete();
        }

        // Add rate limit headers to response
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", 
            String.valueOf(bucket.getRemainingTokens()));

        return chain.filter(exchange);
    }

    private String extractClientId(ServerWebExchange exchange) {
        // Try to get user ID from principal
        String userId = exchange.getPrincipal()
            .map(principal -> principal.getName())
            .block();

        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        // Fall back to IP address
        String ipAddress = exchange.getRequest().getRemoteAddress() != null
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";

        return ipAddress;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    /**
     * Simple token bucket implementation for rate limiting
     */
    private static class RateLimitBucket {
        private final int capacity;
        private final AtomicInteger tokens;
        private volatile Instant lastRefillTime;
        private final Duration refillInterval = Duration.ofMinutes(1);

        public RateLimitBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = Instant.now();
        }

        public synchronized boolean tryConsume() {
            refillIfNeeded();

            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }

            return false;
        }

        private void refillIfNeeded() {
            Instant now = Instant.now();
            if (Duration.between(lastRefillTime, now).compareTo(refillInterval) >= 0) {
                tokens.set(capacity);
                lastRefillTime = now;
            }
        }

        public int getRemainingTokens() {
            refillIfNeeded();
            return tokens.get();
        }

        public Instant getResetTime() {
            return lastRefillTime.plus(refillInterval);
        }
    }
}
