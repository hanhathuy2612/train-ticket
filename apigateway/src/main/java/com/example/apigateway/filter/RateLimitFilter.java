package com.example.apigateway.filter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Simple in-memory rate limiting filter
 * 
 * For production, use Redis-based rate limiting with Spring Cloud Gateway's
 * built-in RequestRateLimiter or Bucket4j
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    
    // Rate limit configuration
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 100;
    private static final int AUTH_REQUESTS_PER_MINUTE = 10; // Stricter for auth endpoints
    private static final long WINDOW_SIZE_MS = 60000; // 1 minute window
    
    // In-memory rate limit storage (use Redis in production)
    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // Get rate limit key
        String key = getRateLimitKey(request);
        int maxRequests = getMaxRequests(path);
        
        // Check rate limit
        RateLimitEntry entry = rateLimitMap.compute(key, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || now - v.windowStart.get() > WINDOW_SIZE_MS) {
                return new RateLimitEntry(now);
            }
            return v;
        });
        
        int currentCount = entry.count.incrementAndGet();
        
        if (currentCount > maxRequests) {
            logger.warn("Rate limit exceeded for key: {} (count: {}, max: {})", key, currentCount, maxRequests);
            return onRateLimitExceeded(exchange);
        }
        
        // Add rate limit headers
        ServerHttpResponse response = exchange.getResponse();
        long remaining = Math.max(0, maxRequests - currentCount);
        long resetTime = (entry.windowStart.get() + WINDOW_SIZE_MS - System.currentTimeMillis()) / 1000;
        
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(Math.max(0, resetTime)));
        
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -150; // Between logging and authentication
    }

    private String getRateLimitKey(ServerHttpRequest request) {
        // Prefer user ID if available
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }
        
        // Fallback to IP address
        String ip = getClientIp(request);
        return "ip:" + ip;
    }

    private int getMaxRequests(String path) {
        // Stricter limits for authentication endpoints
        if (path.contains("/login") || path.contains("/register") || 
            path.contains("/forgot-password") || path.contains("/reset-password")) {
            return AUTH_REQUESTS_PER_MINUTE;
        }
        return DEFAULT_REQUESTS_PER_MINUTE;
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
    }

    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("Retry-After", "60");
        
        String body = "{\"success\":false,\"message\":\"Rate limit exceeded. Please try again later.\",\"statusCode\":429}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    // Inner class for rate limit tracking
    private static class RateLimitEntry {
        final AtomicLong windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart) {
            this.windowStart = new AtomicLong(windowStart);
            this.count = new AtomicInteger(0);
        }
    }
}

