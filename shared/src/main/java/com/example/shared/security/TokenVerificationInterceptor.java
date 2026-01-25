package com.example.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to verify token signature for requests coming from API Gateway
 * <p>
 * This provides an additional security layer in the hybrid approach:
 * - Gateway validates token fully
 * - Services verify token signature (lightweight check)
 * <p>
 * Usage: Add this interceptor to your WebMvcConfigurer
 *
 * @Configuration public class WebConfig implements WebMvcConfigurer {
 * @Autowired private TokenVerificationInterceptor;
 * @Override public void addInterceptors(InterceptorRegistry registry) {
 * registry.addInterceptor(tokenVerificationInterceptor)
 * .addPathPatterns("/**")
 * .excludePathPatterns("/actuator/**", "/health");
 * }
 * }
 */
@Slf4j
@Component
public class TokenVerificationInterceptor implements HandlerInterceptor {
    private final TokenVerificationUtility tokenVerificationUtility;

    /**
     * Whether to enable token verification
     * Set too false to trust headers from gateway (for internal network)
     */
    private boolean enabled;

    public TokenVerificationInterceptor(TokenVerificationUtility tokenVerificationUtility) {
        this.tokenVerificationUtility = tokenVerificationUtility;
        // Default to enabled, can be overridden via setEnabled() from WebConfig
        // Check system property or environment variable as fallback
        String enableVerification = System.getProperty("token.verification.enabled", 
                System.getenv().getOrDefault("TOKEN_VERIFICATION_ENABLED", "true"));
        this.enabled = Boolean.parseBoolean(enableVerification);
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        // Skip verification if disabled (trust gateway headers)
        if (!enabled || tokenVerificationUtility == null) {
            return true;
        }

        // Get token from header (forwarded by gateway)
        String token = request.getHeader("X-Auth-Token");

        // If no token, might be a public endpoint or request bypassed gateway
        // Log warning but don't block (let controller handle it)
        if (token == null || token.isEmpty()) {
            log.debug("No X-Auth-Token header found for path: {}", request.getRequestURI());
            // Don't block - might be a public endpoint or internal service call
            return true;
        }

        // Verify token signature
        boolean isValid = tokenVerificationUtility.verifyTokenSignature(token);

        if (!isValid) {
            log.warn("Token signature verification failed for path: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Token verification failed\",\"statusCode\":401}");
            return false;
        }

        log.debug("Token signature verified successfully for path: {}", request.getRequestURI());
        return true;
    }

    /**
     * Enable or disable token verification
     * Set to false if services are in a trusted network and only receive requests
     * from gateway
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
