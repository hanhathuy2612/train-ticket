package com.example.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration for API Gateway (WebFlux Reactive Stack)
 * <p>
 * This configuration uses Spring Security WebFlux for reactive applications.
 * <p>
 * IMPORTANT: This gateway uses CUSTOM JwtAuthenticationFilter (GlobalFilter)
 * for authentication, NOT Spring Security OAuth2 Resource Server. Therefore:
 * <p>
 * 1. OAuth2 Resource Server is NOT enabled in SecurityWebFilterChain
 * 2. All requests are permitted to pass through to Gateway filters
 * 3. Authentication is handled by JwtAuthenticationFilter (GlobalFilter)
 * - JwtAuthenticationFilter validates JWT tokens with Keycloak
 * - Extracts user info (userId, username, roles) from token
 * - Adds headers: X-User-Id, X-User-Name, X-User-Roles, X-Auth-Token
 * - Blocks unauthorized requests (401)
 * <p>
 * CORS is configured using CorsConfigurationSource bean.
 * This ensures CORS is handled by Spring Security BEFORE Gateway filters.
 * <p>
 * WebFlux Security Best Practices:
 * - Use @EnableWebFluxSecurity (not @EnableWebSecurity)
 * - Use ServerHttpSecurity (not HttpSecurity)
 * - Use authorizeExchange() (not authorizeRequests())
 * - Use pathMatchers() with ServerWebExchange
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * CORS Configuration Source
     * This bean provides CORS configuration to Spring Security.
     * It handles both preflight OPTIONS requests and actual requests.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins (cannot use "*" with allowCredentials: true)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://localhost:3000"));

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Allowed headers
        configuration.setAllowedHeaders(List.of(
            "*"));

        // Exposed headers (headers that browser can access)
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-User-Id",
            "X-User-Name",
            "X-User-Roles",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Max age for preflight cache (in seconds)
        configuration.setMaxAge(3600L);

        // Apply to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Security Web Filter Chain for WebFlux
     * <p>
     * This configures Spring Security for reactive WebFlux applications.
     * Since we use custom JwtAuthenticationFilter for authentication,
     * we permit all requests to pass through to Gateway filters.
     * <p>
     * Filter Order:
     * 1. CORS (handled by Spring Security first)
     * 2. CSRF (disabled for stateless API)
     * 3. Authorization (all permitted to pass to Gateway filters)
     * 4. Gateway Filters (LoggingFilter, RateLimitFilter, JwtAuthenticationFilter)
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Configure CORS - must be before authorization
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Disable CSRF for stateless REST API
            // CSRF protection is not needed for stateless APIs using JWT tokens
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            // Disable HTTP Basic Authentication (we use JWT)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            // Disable form login (we use JWT)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            // Disable logout (stateless API)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            // Authorization rules
            // All requests are permitted to pass through to Gateway filters
            // Authentication is handled by JwtAuthenticationFilter (GlobalFilter)
            //
            // NOTE: .pathMatchers(...).permitAll() is NOT needed here because:
            // 1. .anyExchange().permitAll() already permits all requests
            // 2. Spring Security doesn't validate JWT tokens (no OAuth2 Resource Server)
            // 3. JwtAuthenticationFilter is the SINGLE SOURCE OF TRUTH for public endpoints
            //  are defined in JwtAuthenticationFilter.PUBLIC_PATHS
            .authorizeExchange(ex -> ex
                // Allow all OPTIONS requests (CORS preflight)
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // All other requests are permitted to pass through to Gateway filters
                // JwtAuthenticationFilter will handle authentication and decide which
                // endpoints are public vs protected based on PUBLIC_PATHS list
                .anyExchange().permitAll())
            // Build the security filter chain
            .build();
    }
}
