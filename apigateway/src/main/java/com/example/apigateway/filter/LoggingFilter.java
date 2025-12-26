package com.example.apigateway.filter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Global logging filter for API Gateway
 * 
 * Logs all incoming requests and outgoing responses with timing information
 * Adds correlation ID for request tracing across microservices
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String START_TIME_ATTRIBUTE = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Generate or extract correlation ID
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // Record start time
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(START_TIME_ATTRIBUTE, startTime);
        
        // Extract request details
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String userId = request.getHeaders().getFirst("X-User-Id");
        
        // Log incoming request
        logger.info("Incoming request: {} {} | Client: {} | User: {} | Correlation: {}", 
                method, path, clientIp, userId != null ? userId : "anonymous", correlationId);
        
        // Add correlation ID to request headers
        final String finalCorrelationId = correlationId;
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    // Log outgoing response
                    ServerHttpResponse response = exchange.getResponse();
                    Long startTimeValue = exchange.getAttribute(START_TIME_ATTRIBUTE);
                    long duration = startTimeValue != null 
                            ? System.currentTimeMillis() - startTimeValue : 0;
                    
                    int statusCode = response.getStatusCode() != null 
                            ? response.getStatusCode().value() : 0;
                    
                    if (statusCode >= 400) {
                        logger.warn("Response: {} {} | Status: {} | Duration: {}ms | Correlation: {}", 
                                method, path, statusCode, duration, finalCorrelationId);
                    } else {
                        logger.info("Response: {} {} | Status: {} | Duration: {}ms | Correlation: {}", 
                                method, path, statusCode, duration, finalCorrelationId);
                    }
                }));
    }

    @Override
    public int getOrder() {
        return -200; // Higher priority than authentication filter
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
    }
}

