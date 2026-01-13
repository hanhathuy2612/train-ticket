package com.example.apigateway.config;

public final class Constants {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String IP_HEADER = "X-Forwarded-For";
    public static final String X_REAL_IP_HEADER = "X-Real-IP";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String RETRY_AFTER_HEADER = "Retry-After";
    public static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    public static final String START_TIME_ATTRIBUTE = "startTime";
    public static final String UNKNOWN = "unknown";

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTH_TOKEN_HEADER = "X-Auth-Token"; // Forward token to services for verification
}
