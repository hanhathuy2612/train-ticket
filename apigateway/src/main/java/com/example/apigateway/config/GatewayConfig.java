package com.example.apigateway.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
	// Configuration for API Gateway
	// Redis is not needed for JWT validation
	// If rate limiting with Redis is needed in the future, add
	// ReactiveRedisTemplate here
}
