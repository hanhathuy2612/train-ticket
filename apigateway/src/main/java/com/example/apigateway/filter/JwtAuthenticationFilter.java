package com.example.apigateway.filter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.example.apigateway.util.JwtUtil;

import reactor.core.publisher.Mono;
/**
 /**
  * JwtAuthenticationFilter is a custom filter for the Spring Cloud Gateway
  * that performs JWT (JSON Web Token) authentication on incoming HTTP requests.
  * 
  * This filter is annotated with @Component, making it a Spring-managed bean
  * and enabling it to be automatically picked up and applied to gateway requests.
  * 
  * Main responsibilities and internal workflow:
  * 
  * 1. **Dependency Injection:**
  *    - Uses Spring's @Autowired to inject a JwtUtil instance, which is
  *      responsible for actual JWT token operations (parsing, validating, extracting info).
  * 
  * 2. **Exclusion Paths:**
  *    - Defines a static list (EXCLUDED_PATHS) of endpoint paths (such as user registration,
  *      login, and actuator endpoints) which do not require authentication. Any request to
  *      a matching path is allowed through without JWT validation.
  * 
  * 3. **JWT Validation:**
  *    - On each request, checks the URL path.
  *    - If the path is not excluded, it attempts to extract the Authorization header.
  *    - The filter expects an "Authorization" header of the form "Bearer <token>".
  *    - If the header is missing or malformed, the filter responds immediately with
  *      HTTP 401 Unauthorized.
  *    - Extracts the JWT token substring and validates it using JwtUtil.
  *    - If the token is invalid (expired, malformed, incorrect signature, etc.), the filter
  *      returns HTTP 401 Unauthorized.
  * 
  * 4. **Propagating Authentication Context:**
  *    - If the JWT is valid, extracts the username and userId from the JWT using JwtUtil.
  *    - Augments the request headers with "X-User-Name" and "X-User-Id" headers
  *      so that downstream microservices can know the authenticated user's identity
  *      without re-parsing the JWT.
  *    - Passes the mutated request further down the gateway filter chain.
  * 
  * 5. **Order:**
  *    - Implements the Ordered interface, which allows control over the filter’s execution
  *      order relative to other gateway filters.
  * 
  * This class provides a crucial security entry point in the cloud-native, microservices
  * architecture: it ensures only authenticated requests reach the protected routes, while
  * unprotected/excluded routes are still accessible for user registration, login, or
  * readiness checks.
  */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

	@Autowired
	private JwtUtil jwtUtil;

	private static final List<String> EXCLUDED_PATHS = List.of(
			"/api/users/register",
			"/api/users/login",
			"/actuator");

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getURI().getPath();

		// Skip authentication for excluded paths
		if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
			return chain.filter(exchange);
		}

		String authHeader = request.getHeaders().getFirst("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			ServerHttpResponse response = exchange.getResponse();
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return response.setComplete();
		}

		String token = authHeader.substring(7);

		if (!jwtUtil.validateToken(token)) {
			ServerHttpResponse response = exchange.getResponse();
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return response.setComplete();
		}

		// Add user info to headers for downstream services
		String username = jwtUtil.extractUsername(token);
		Long userId = jwtUtil.extractUserId(token);
		ServerHttpRequest modifiedRequest = request.mutate()
				.header("X-User-Name", username)
				.header("X-User-Id", userId != null ? userId.toString() : "")
				.build();

		return chain.filter(exchange.mutate().request(modifiedRequest).build());
	}

	@Override
	public int getOrder() {
		return -100;
	}
}
