package com.example.apigateway.filter;

import java.util.List;
import java.util.Set;

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

import com.example.apigateway.config.Constants;
import com.example.apigateway.util.KeycloakTokenValidator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Keycloak Authentication Filter for Spring Cloud Gateway
 * 
 * This filter validates Keycloak JWT tokens for protected routes and adds user
 * context
 * to request headers for downstream services.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final KeycloakTokenValidator keycloakTokenValidator;

    // Paths that don't require authentication
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/users/register",
            "/api/users/login",
            "/api/users/forgot-password",
            "/api/users/reset-password",
            "/api/users/refresh-token",
            "/api/inventory/health",
            "/api/tickets/health",
            "/api/payments/health",
            "/api/notifications/health",
            "/actuator");

    // Paths that require admin role
    private static final List<String> ADMIN_PATHS = List.of(
            "/api/users/roles",
            "/api/users/status",
            "/api/inventory/routes",
            "/api/inventory/trains",
            "/api/inventory/schedules/bulk",
            "/api/payments/stats",
            "/api/notifications/retry-failed");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();

        logger.debug("Processing request: {} {}", method, path);

        // Skip authentication for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            logger.debug("Skipping authentication for OPTIONS preflight request: {}", path);
            return chain.filter(exchange);
        }

        // Skip authentication for excluded paths
        if (isExcludedPath(path)) {
            logger.debug("Skipping authentication for excluded path: {}", path);
            return chain.filter(exchange);
        }

        // Extract and validate token
        String authHeader = request.getHeaders().getFirst(Constants.AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(Constants.BEARER_PREFIX)) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            return onUnauthorized(exchange, "Missing or invalid authorization token");
        }

        String token = authHeader.substring(Constants.BEARER_PREFIX.length());

        try {
            if (!keycloakTokenValidator.validateToken(token)) {
                logger.warn("Invalid or expired Keycloak token for path: {}", path);
                return onUnauthorized(exchange, "Token is invalid or expired");
            }

            // Extract user information from Keycloak token
            String username = keycloakTokenValidator.extractUsername(token);
            String userId = keycloakTokenValidator.extractUserId(token);
            Set<String> roles = keycloakTokenValidator.extractRoles(token);

            logger.debug("Authenticated user: {} (id: {}) for path: {}", username, userId, path);

            // Check admin access for restricted paths
            // Keycloak roles might be prefixed with realm or client name
            boolean hasAdminRole = roles.stream()
                    .anyMatch(role -> role.equals("ADMIN") || role.equals("admin") ||
                            role.endsWith(":ADMIN") || role.endsWith(":admin"));

            if (isAdminPath(path) && !hasAdminRole) {
                logger.warn("Forbidden access attempt by user {} to admin path: {}", username, path);
                return onForbidden(exchange, "Access denied. Admin role required.");
            }

            // Add user context AND token to headers for downstream services
            // Services can verify token signature if needed (hybrid approach)
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header(Constants.AUTH_TOKEN_HEADER, token) // Forward token for verification
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Name", username != null ? username : "")
                    .header("X-User-Roles", String.join(",", roles))
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            logger.error("Error processing Keycloak token", e);
            return onUnauthorized(exchange, "Error processing authentication token");
        }
    }

    @Override
    public int getOrder() {
        return -100; // High priority - runs before other filters
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isAdminPath(String path) {
        return ADMIN_PATHS.stream().anyMatch(path::contains);
    }

    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        addCorsHeaders(response, exchange.getRequest());
        String body = String.format("{\"success\":false,\"message\":\"%s\",\"statusCode\":401}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    private Mono<Void> onForbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        addCorsHeaders(response, exchange.getRequest());
        String body = String.format("{\"success\":false,\"message\":\"%s\",\"statusCode\":403}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    /**
     * Add CORS headers to response to allow cross-origin requests
     * IMPORTANT: Cannot use "*" with allowCredentials: true
     */
    private void addCorsHeaders(ServerHttpResponse response, ServerHttpRequest request) {
        String origin = request.getHeaders().getFirst("Origin");
        if (origin != null && !origin.isEmpty()) {
            // Echo back the origin (allows credentials)
            response.getHeaders().add("Access-Control-Allow-Origin", origin);
            response.getHeaders().add("Access-Control-Allow-Credentials", "true");
        } else {
            // No origin header - use "*" but cannot use credentials
            response.getHeaders().add("Access-Control-Allow-Origin", "*");
        }
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        response.getHeaders().add("Access-Control-Allow-Headers",
                "Content-Type, Authorization, X-Requested-With, X-User-Id, X-Correlation-Id, Accept, Origin");
        response.getHeaders().add("Access-Control-Max-Age", "3600");
    }
}
