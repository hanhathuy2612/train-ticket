package com.example.apigateway.filter;

import com.example.apigateway.config.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global logging filter for API Gateway
 * Logs all incoming requests and outgoing responses with timing information
 * Adds correlation ID for request tracing across microservices
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

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
        log.info("Incoming request: {} {} | Client: {} | User: {} | Correlation: {} | User-Agent: {}",
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
                        log.warn(
                                "Response: {} {} | Status: {} | Duration: {}ms | Correlation: {} | Client: {} | User: {} | User-Agent: {}",
                                method, path, statusCode, duration, finalCorrelationId, clientIp, userId, userAgent);
                    } else {
                        log.info(
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
