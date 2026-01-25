package com.example.apigateway.filter;

import com.example.apigateway.config.Constants;
import com.example.apigateway.util.KeycloakTokenValidator;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for API Gateway
 * <p>
 * This filter validates JWT tokens from Keycloak and extracts user information.
 * It runs after LoggingFilter and RateLimitFilter but before routing.
 * <p>
 * Responsibilities:
 * 1. Extract JWT token from Authorization header
 * 2. Validate token with Keycloak (signature, issuer, audience, expiration)
 * 3. Extract user info (userId, username, roles) from token claims
 * 4. Add headers: X-User-Id, X-User-Name, X-User-Roles, X-Auth-Token
 * 5. Block unauthorized requests (401)
 * 6. Skip authentication for public endpoints
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final KeycloakTokenValidator tokenValidator;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        // User service public endpoints
        "/api/users/register",
        "/api/users/login",
        "/api/users/forgot-password",
        "/api/users/reset-password",
        "/api/users/refresh-token",

        // Health check endpoints
        "/api/inventory/health",
        "/api/tickets/health",
        "/api/payments/health",
        "/api/notifications/health",

        // Public inventory endpoints
        "/api/inventory/schedules/availability",

        // Actuator endpoints
        "/actuator",

        // OpenAPI/Swagger endpoints
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        // OpenAPI endpoints from microservices (proxied through Gateway)
        "/api/users/v3/api-docs",
        "/api/tickets/v3/api-docs",
        "/api/inventory/v3/api-docs",
        "/api/payments/v3/api-docs",
        "/api/notifications/v3/api-docs",

        // Fallback endpoints
        "/fallback");

    public JwtAuthenticationFilter(KeycloakTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();

        logger.debug("JwtAuthenticationFilter: Processing request {} {}", method, path);

        // Skip authentication for public endpoints
        if (isPublicPath(path)) {
            logger.debug("Skipping authentication for public path: {}", path);
            return chain.filter(exchange);
        }

        // Extract token from Authorization header
        String authHeader = request.getHeaders().getFirst(Constants.AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(Constants.BEARER_PREFIX)) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            return onUnauthorized(exchange, "Missing or invalid authorization token");
        }

        String token = authHeader.substring(Constants.BEARER_PREFIX.length());

        // Validate token with Keycloak
        // Use switchIfEmpty BEFORE flatMap to handle empty case properly
        return tokenValidator.validateToken(token)
            .switchIfEmpty(Mono.defer(() -> {
                logger.warn("Token validation failed for path: {}", path);
                return Mono.error(new RuntimeException("Token is invalid or expired"));
            }))
            .flatMap(claims -> {
                // Extract user information from claims
                String userId = extractUserId(claims);
                String username = extractUsername(claims);
                List<String> roles = extractAndAddRoles(claims);

                logger.debug("Token validated successfully. User: {} (ID: {}), Roles: {}",
                    username, userId, roles);

                // Add user info headers to request
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header(Constants.USER_ID_HEADER, userId)
                    .header(Constants.USER_NAME_HEADER, username)
                    .header(Constants.USER_ROLES_HEADER, String.join(",", roles))
                    .header(Constants.AUTH_TOKEN_HEADER, token) // Forward token to microservice
                    .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            })
            .onErrorResume(e -> {
                logger.error("Error during token validation for path: {}", path, e);
                String message = e.getMessage() != null && e.getMessage().contains("invalid or expired")
                    ? "Token is invalid or expired"
                    : "Token validation error";
                return onUnauthorized(exchange, message);
            });
    }

    @Override
    public int getOrder() {
        return -100; // After LoggingFilter (-200) and RateLimitFilter (-150), before routing
    }

    /**
     * Check if path is public (doesn't require authentication)
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
            .anyMatch(publicPath -> path.equals(publicPath) || path.startsWith(publicPath + "/"));
    }

    /**
     * Extract user ID from JWT claims
     * Keycloak uses "sub" (subject) claim for user ID
     */
    private String extractUserId(Claims claims) {
        String sub = claims.getSubject();
        if (sub != null && !sub.isEmpty()) {
            // Keycloak subject format: "f:uuid:username" or just UUID
            // Extract UUID part if it contains colons
            if (sub.contains(":")) {
                String[] parts = sub.split(":");
                return parts[parts.length - 1]; // Get last part (UUID or username)
            }
            return sub;
        }
        return Constants.UNKNOWN;
    }

    /**
     * Extract username from JWT claims
     * Keycloak uses "preferred_username" claim
     */
    private String extractUsername(Claims claims) {
        // Try preferred_username first
        Object preferredUsername = claims.get("preferred_username");
        if (preferredUsername != null) {
            return preferredUsername.toString();
        }

        // Fallback to email
        Object email = claims.get("email");
        if (email != null) {
            return email.toString();
        }

        // Fallback to subject
        return extractUserId(claims);
    }

    /**
     * Extract roles from JWT claims
     * Keycloak roles are in:
     * - realm_access.roles (realm roles)
     * - resource_access.<client-id>.roles (client roles)
     */
    private List<String> extractAndAddRoles(Claims claims) {
        List<String> roles = new ArrayList<>();

        try {
            // Extract realm roles
            Object realmAccess = claims.get("realm_access");
            extractAndAddRoles(realmAccess, roles);

            // Extract client roles
            Object resourceAccess = claims.get("resource_access");
            if (resourceAccess instanceof Map<?, ?> resourceAccessMap) {
                for (Object clientRolesObj : resourceAccessMap.values()) {
                    extractAndAddRoles(clientRolesObj, roles);
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting roles from token claims", e);
        }

        return roles.stream().distinct().collect(Collectors.toList());
    }

    private void extractAndAddRoles(Object resource, List<String> roles) {
        if (resource instanceof Map<?, ?> map) {
            Object clientRoles = map.get("roles");
            if (clientRoles instanceof List<?> list) {
                list.forEach(item -> {
                    if (item instanceof String role) {
                        roles.add(role);
                    }
                });
            }
        }
    }

    /**
     * Handle unauthorized requests
     */
    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Add CORS headers to error response
        addCorsHeaders(response.getHeaders(), exchange.getRequest());

        String responseBody = String.format(
            "{\"success\":false,\"message\":\"%s\",\"statusCode\":401,\"errorCode\":\"UNAUTHORIZED\"}",
            message);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBody.getBytes())));
    }

    /**
     * Add CORS headers to error response
     */
    private void addCorsHeaders(org.springframework.http.HttpHeaders headers, ServerHttpRequest request) {
        String origin = request.getHeaders().getFirst(org.springframework.http.HttpHeaders.ORIGIN);

        if (origin != null && !origin.isEmpty()) {
            headers.add(org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            headers.add(org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        } else {
            headers.add(org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }

        headers.add(org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        headers.add(org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
            "Content-Type, Authorization, X-Requested-With, X-User-Id, X-Correlation-Id, Accept, Origin");
        headers.add(org.springframework.http.HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
    }
}
