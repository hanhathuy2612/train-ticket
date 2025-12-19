package com.example.apigateway.filter;

import com.example.apigateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

	@Autowired
	private JwtUtil jwtUtil;

	private static final List<String> EXCLUDED_PATHS = List.of(
			"/api/users/register",
			"/api/users/login",
			"/actuator"
	);

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

