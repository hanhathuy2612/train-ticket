package com.example.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * CORS Filter for Spring Cloud Gateway
 * 
 * Handles CORS preflight requests and adds CORS headers to all responses.
 * This filter runs early (high priority) to ensure CORS headers are always
 * present.
 */
@Component
public class CorsFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CorsFilter.class);

    // Allowed methods
    private static final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, PATCH, OPTIONS";

    // Allowed headers
    private static final String ALLOWED_HEADERS = "Content-Type, Authorization, X-Requested-With, X-User-Id, X-Correlation-Id, Accept, Origin";

    // Max age for preflight cache
    private static final String MAX_AGE = "3600";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();

        // Get origin from request
        String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);

        logger.debug("CORS Filter - Origin: {}, Method: {}, Path: {}",
                origin, request.getMethod(), request.getURI().getPath());

        // IMPORTANT: Cannot use "*" with allowCredentials: true
        // Must use specific origin or echo back the request origin
        if (origin != null && !origin.isEmpty()) {
            // Echo back the origin (allows credentials)
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        } else {
            // No origin header (e.g., same-origin request or Postman)
            // Use "*" but cannot use credentials
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }

        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, ALLOWED_HEADERS);
        headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, MAX_AGE);
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                "X-User-Id, X-User-Name, X-User-Roles, X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset");

        // Handle preflight OPTIONS request
        if (request.getMethod() == HttpMethod.OPTIONS) {
            logger.debug("Handling CORS preflight request for path: {}", request.getURI().getPath());
            response.setStatusCode(HttpStatus.OK);
            return response.setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run before all other filters to handle CORS preflight first
        // Must be lower than LoggingFilter (-200) and JwtAuthenticationFilter (-100)
        return -300;
    }
}
