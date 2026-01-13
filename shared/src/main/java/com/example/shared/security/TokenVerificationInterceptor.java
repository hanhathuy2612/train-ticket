package com.example.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor to verify token signature for requests coming from API Gateway
 * 
 * This provides an additional security layer in the hybrid approach:
 * - Gateway validates token fully
 * - Services verify token signature (lightweight check)
 * 
 * Usage: Add this interceptor to your WebMvcConfigurer
 * 
 * @Configuration
 *                public class WebConfig implements WebMvcConfigurer {
 * @Autowired
 *            private TokenVerificationInterceptor tokenVerificationInterceptor;
 * 
 * @Override
 *           public void addInterceptors(InterceptorRegistry registry) {
 *           registry.addInterceptor(tokenVerificationInterceptor)
 *           .addPathPatterns("/**")
 *           .excludePathPatterns("/actuator/**", "/health");
 *           }
 *           }
 */
@Component
public class TokenVerificationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TokenVerificationInterceptor.class);

    @Autowired(required = false)
    private TokenVerificationUtility tokenVerificationUtility;

    /**
     * Whether to enable token verification
     * Set to false to trust headers from gateway (for internal network)
     */
    private boolean enabled = true;

    public TokenVerificationInterceptor() {
        // Check if we're in a trusted environment (e.g., internal network)
        String enableVerification = System.getProperty("token.verification.enabled", "true");
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
            logger.debug("No X-Auth-Token header found for path: {}", request.getRequestURI());
            // Don't block - might be a public endpoint or internal service call
            return true;
        }

        // Verify token signature
        boolean isValid = tokenVerificationUtility.verifyTokenSignature(token);

        if (!isValid) {
            logger.warn("Token signature verification failed for path: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Token verification failed\",\"statusCode\":401}");
            return false;
        }

        logger.debug("Token signature verified successfully for path: {}", request.getRequestURI());
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
