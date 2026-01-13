package com.example.inventoryservice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor to verify token signature for requests coming from API Gateway
 * 
 * This provides an additional security layer in the hybrid approach:
 * - Gateway validates token fully
 * - Services verify token signature (lightweight check)
 */
public class TokenVerificationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TokenVerificationInterceptor.class);

    private final TokenVerificationUtility tokenVerificationUtility;
    private final boolean enabled;

    public TokenVerificationInterceptor(TokenVerificationUtility tokenVerificationUtility, 
                                       @Value("${token.verification.enabled:true}") boolean enabled) {
        this.tokenVerificationUtility = tokenVerificationUtility;
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {

        // Skip verification if disabled (trust gateway headers)
        if (!enabled) {
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
}
