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

import com.example.apigateway.config.Constants;

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

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate or extract correlation ID
        String correlationId = request.getHeaders().getFirst(Constants.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Record start time
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(Constants.START_TIME_ATTRIBUTE, startTime);

        // Extract request details
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeaders().getFirst(Constants.USER_AGENT_HEADER);
        String userId = request.getHeaders().getFirst(Constants.USER_ID_HEADER);

        // Log incoming request
        logger.info("Incoming request: {} {} | Client: {} | User: {} | Correlation: {} | User-Agent: {}",
                method, path, clientIp, userId != null ? userId : "anonymous", correlationId, userAgent);

        // Add correlation ID to request headers
        final String finalCorrelationId = correlationId;
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(Constants.CORRELATION_ID_HEADER, finalCorrelationId)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    // Log outgoing response
                    ServerHttpResponse response = exchange.getResponse();
                    Long startTimeValue = exchange.getAttribute(Constants.START_TIME_ATTRIBUTE);
                    long duration = startTimeValue != null
                            ? System.currentTimeMillis() - startTimeValue
                            : 0;

                    int statusCode = response.getStatusCode() != null
                            ? response.getStatusCode().value()
                            : 0;

                    if (statusCode >= 400) {
                        logger.warn(
                                "Response: {} {} | Status: {} | Duration: {}ms | Correlation: {} | Client: {} | User: {} | User-Agent: {}",
                                method, path, statusCode, duration, finalCorrelationId, clientIp, userId, userAgent);
                    } else {
                        logger.info(
                                "Response: {} {} | Status: {} | Duration: {}ms | Correlation: {} | Client: {} | User: {} | User-Agent: {}",
                                method, path, statusCode, duration, finalCorrelationId, clientIp, userId, userAgent);
                    }
                }));
    }

    @Override
    public int getOrder() {
        return -200; // Higher priority than authentication filter
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst(Constants.IP_HEADER);
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeaders().getFirst(Constants.X_REAL_IP_HEADER);
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null) {
            return Constants.UNKNOWN;
        }
        var address = remoteAddress.getAddress();
        if (address == null) {
            return Constants.UNKNOWN;
        }
        return address.getHostAddress();
    }
}
