package com.example.apigateway.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Security Configuration for API Gateway
 * <p>
 * IMPORTANT: This gateway uses CUSTOM JwtAuthenticationFilter (GlobalFilter)
 * for authentication,
 * NOT Spring Security OAuth2 Resource Server. Therefore:
 * <p>
 * 1. OAuth2 Resource Server is DISABLED (commented out)
 * 2. All requests are permitted to pass through to Gateway filters
 * 3. Authentication is handled by JwtAuthenticationFilter
 * <p>
 * CORS is configured directly in SecurityConfig using CorsConfigurationSource
 * bean.
 * This ensures CORS is handled by Spring Security BEFORE Gateway filters.
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

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF for stateless API
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Authorization rules
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(
                                "/api/users/register",
                                "/api/users/login",
                                "/api/users/forgot-password",
                                "/api/users/reset-password",
                                "/api/users/refresh-token",
                                "/api/inventory/health",
                                // "/api/inventory/schedules/search",
                                "/api/inventory/schedules/availability",
                                "/api/tickets/health",
                                "/api/payments/health",
                                "/api/notifications/health",
                                "/actuator/**",
                                // OpenAPI/Swagger endpoints for documentation and debugging
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/webjars/**")
                        .permitAll()
                        // Permit all other requests - authentication is handled by
                        // JwtAuthenticationFilter (GlobalFilter)
                        // NOT by Spring Security OAuth2 Resource Server
                        .anyExchange().permitAll())

                // 🔥 QUAN TRỌNG: Disable OAuth2 Resource Server
                // Vì đang dùng custom JwtAuthenticationFilter (GlobalFilter) để validate
                // Keycloak tokens
                // Nếu enable OAuth2 Resource Server, nó sẽ:
                // 1. Chạy TRƯỚC Gateway filters
                // 2. Yêu cầu authentication cho tất cả requests
                // 3. Chặn OPTIONS requests và excluded paths
                //
                // Uncomment only if you want to use Spring Security OAuth2 Resource Server
                // instead:
                // .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))

                .build();
    }
}
