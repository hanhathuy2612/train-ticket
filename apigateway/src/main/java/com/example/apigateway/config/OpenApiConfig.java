package com.example.apigateway.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * Provides API documentation and helps debug CORS issues
 * Access Swagger UI at: <a href="http://localhost:8080/swagger-ui.html">...</a>
 * Access API docs at: <a href="http://localhost:8080/v3/api-docs">...</a>
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Train Ticket API Gateway", version = "1.0.0", description = "API Gateway for Train Ticket Booking System with Keycloak JWT Authentication", contact = @Contact(name = "API Support", email = "support@trainticket.com"), license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")), servers = {
        @Server(url = "http://localhost:8080", description = "Local Development Server")
})
public class OpenApiConfig {
    // Configuration is done via annotations
    // Additional configuration can be added via application.yml
}
