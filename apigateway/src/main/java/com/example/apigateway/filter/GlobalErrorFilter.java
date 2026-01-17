package com.example.apigateway.filter;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.example.apigateway.config.Constants;

import reactor.core.publisher.Mono;

/**
 * Global error handler for API Gateway
 *
 * Provides consistent error responses for all types of exceptions
 */
@Component
@Order(-1)
public class GlobalErrorFilter implements ErrorWebExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalErrorFilter.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        String path = exchange.getRequest().getURI().getPath();
        String correlationId = exchange.getRequest().getHeaders().getFirst(Constants.CORRELATION_ID_HEADER);

        HttpStatus status;
        String message;
        String errorCode;

        if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
            errorCode = "GATEWAY_" + status.value();
        } else if (ex instanceof ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service is temporarily unavailable. Please try again later.";
            errorCode = "SERVICE_UNAVAILABLE";
            logger.error("Service connection error for path {}: {}", path, ex.getMessage());
        } else if (ex instanceof TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Request timed out. Please try again.";
            errorCode = "GATEWAY_TIMEOUT";
            logger.error("Request timeout for path {}: {}", path, ex.getMessage());
        } else if (ex.getCause() instanceof ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Downstream service is unavailable. Please try again later.";
            errorCode = "DOWNSTREAM_UNAVAILABLE";
            logger.error("Downstream service unavailable for path {}: {}", path, ex.getMessage());
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred. Please try again.";
            errorCode = "INTERNAL_ERROR";
            logger.error("Unexpected error for path {}: ", path, ex);
        }

        if (!response.isCommitted()) {
            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            // Add CORS headers to error response
            // This ensures browser can read error responses even when CORS fails
            addCorsHeaders(response.getHeaders(), exchange.getRequest());

            String responseBody = String.format(
                    "{\"success\":false,\"message\":\"%s\",\"statusCode\":%d,\"errorCode\":\"%s\"," +
                            "\"path\":\"%s\",\"correlationId\":\"%s\",\"timestamp\":\"%s\"}",
                    message, status.value(), errorCode, path,
                    correlationId != null ? correlationId : "N/A",
                    LocalDateTime.now().toString());

            return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBody.getBytes())));
        }

        return Mono.empty();
    }

    /**
     * Add CORS headers to error response
     * This ensures error responses (500, 503, 504, etc.) also have CORS headers
     */
    private void addCorsHeaders(HttpHeaders headers, ServerHttpRequest request) {
        String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);

        if (origin != null && !origin.isEmpty()) {
            // Echo back the origin (allows credentials)
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        } else {
            // No origin header - allow all origins but cannot use credentials
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }

        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                "Content-Type, Authorization, X-Requested-With, X-User-Id, X-Correlation-Id, Accept, Origin");
        headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
    }
}
