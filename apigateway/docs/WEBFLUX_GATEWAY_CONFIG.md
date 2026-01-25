# Cấu hình WebFlux Gateway và Security

## Tổng quan

API Gateway này sử dụng **Spring Cloud Gateway** với **WebFlux reactive stack** và **Spring Security WebFlux** cho bảo mật.

## 1. Cấu hình WebFlux Gateway

### 1.1. Cấu hình cơ bản trong `application.yml`

```yaml
spring:
  # Bắt buộc phải dùng reactive stack cho Spring Cloud Gateway
  main:
    web-application-type: reactive
  
  cloud:
    gateway:
      # Bật metrics để monitor
      metrics:
        enabled: true
      
      # Server WebFlux configuration (NEW format for Spring Cloud 2025.0.0+)
      server:
        webflux:
          # Global CORS configuration (MUST be inside server.webflux for Spring Cloud 2025.0.0+)
          # Can also be configured in SecurityConfig for additional control
          globalcors:
            cors-configurations:  # Note: kebab-case, not camelCase
              '[/**]':
                allowedOrigins:
                  - "http://localhost:4200"
                  - "http://localhost:3000"
                allowedMethods:
                  - GET
                  - POST
                  - PUT
                  - DELETE
                  - PATCH
                  - OPTIONS
                allowedHeaders: "*"
                allowCredentials: true
                maxAge: 3600
          # Định nghĩa routes
          routes:
            - id: user-service
              uri: lb://user-service
              predicates:
                - Path=/api/users/**
              filters:
                - StripPrefix=1
                - name: CircuitBreaker
                  args:
                    name: user-service-circuit-breaker
                    fallbackUri: forward:/fallback/user-service
```

### 1.2. Cấu trúc Routes

**⚠️ QUAN TRỌNG: Spring Cloud 2025.0.0+ Breaking Change**

Trong Spring Cloud 2025.0.0, có thay đổi về property prefixes:

| Module | Deprecated Prefix | New Prefix |
|--------|------------------|------------|
| `spring-cloud-starter-gateway-server-webflux` | `spring.cloud.gateway.*` | `spring.cloud.gateway.server.webflux.*` |

**Cấu hình routes MỚI (Spring Cloud 2025.0.0+):**
```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:  # ✅ ĐÚNG cho Spring Cloud 2025.0.0+
            - id: route-id
              uri: lb://service-name
              predicates:
                - Path=/api/**
              filters:
                - StripPrefix=1
```

**Cấu hình routes CŨ (Deprecated):**
```yaml
spring:
  cloud:
    gateway:
      routes:  # ⚠️ DEPRECATED trong Spring Cloud 2025.0.0+
        - id: route-id
          uri: lb://service-name
```

> **Lưu ý:** 
> - Trong Spring Cloud 2025.0.0 trở lên, bạn PHẢI dùng `spring.cloud.gateway.server.webflux.routes`
> - Cấu hình cũ `spring.cloud.gateway.routes` vẫn hoạt động nhưng sẽ hiển thị warning deprecated
> - Có thể dùng `spring-boot-properties-migrator` để tự động migrate

### 1.3. Dependencies trong `build.gradle`

```gradle
dependencies {
    // Spring Cloud Gateway với WebFlux
    implementation('org.springframework.cloud:spring-cloud-starter-gateway') {
        exclude group: 'org.springframework.cloud', module: 'spring-cloud-gateway-server'
    }
    implementation 'org.springframework.cloud:spring-cloud-gateway-server-webflux'
    
    // Eureka cho service discovery
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    
    // Actuator cho monitoring
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // Redis reactive cho rate limiting
    implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
    
    // Circuit Breaker
    implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j'
}
```

## 2. Cấu hình Security cho WebFlux với Keycloak và JwtAuthenticationFilter

### 2.1. Kiến trúc Security (Hybrid Approach)

Gateway này sử dụng **hybrid approach** cho authentication:

1. **Spring Security WebFlux** - Xử lý CORS và cấu hình cơ bản
2. **Custom JwtAuthenticationFilter (GlobalFilter)** - Validate Keycloak JWT tokens
3. **KeycloakTokenValidator** - Verify token signature và extract claims

**Lý do không dùng OAuth2 Resource Server:**
- Cần custom logic để forward token đến downstream services
- Cần extract và forward user context (userId, roles) trong headers
- Linh hoạt hơn trong việc xử lý authentication flow

### 2.2. SecurityConfig với @EnableWebFluxSecurity

```java
@Configuration
@EnableWebFluxSecurity  // ✅ Dùng cho WebFlux, không phải @EnableWebSecurity
public class SecurityConfig {

    /**
     * CORS Configuration Source
     * Xử lý CORS preflight requests và actual requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins (không thể dùng "*" với allowCredentials: true)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://localhost:3000"
        ));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Allowed headers
        configuration.setAllowedHeaders(List.of("*"));
        
        // Exposed headers (headers mà browser có thể đọc)
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-User-Id",
            "X-User-Name",
            "X-User-Roles",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Max age for preflight cache (in seconds)
        configuration.setMaxAge(3600L);
        
        // Apply to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Security Web Filter Chain for WebFlux
     * 
     * Filter Order:
     * 1. CORS (handled by Spring Security first)
     * 2. CSRF (disabled for stateless API)
     * 3. Authorization (all permitted to pass to Gateway filters)
     * 4. Gateway Global Filters (LoggingFilter, RateLimitFilter, JwtAuthenticationFilter)
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Configure CORS - must be before authorization
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Disable CSRF for stateless REST API
            // CSRF protection is not needed for stateless APIs using JWT tokens
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // Disable HTTP Basic Authentication (we use JWT)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            
            // Disable form login (we use JWT)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            
            // Disable logout (stateless API)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            
            // Authorization rules
            // All requests are permitted to pass through to Gateway filters
            // Authentication is handled by JwtAuthenticationFilter (GlobalFilter)
            .authorizeExchange(ex -> ex
                // Allow all OPTIONS requests (CORS preflight)
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Public endpoints that don't require authentication
                .pathMatchers(
                    // User service public endpoints
                    "/api/users/register",
                    "/api/users/login",
                    "/api/users/forgot-password",
                    "/api/users/reset-password",
                    "/api/users/refresh-token",
                    
                    // Health check endpoints
                    "/api/inventory/health",
                    "/api/tickets/health",
                    "/api/payments/health",
                    "/api/notifications/health",
                    
                    // Public inventory endpoints
                    "/api/inventory/schedules/availability",
                    
                    // Actuator endpoints for monitoring
                    "/actuator/**",
                    
                    // OpenAPI/Swagger endpoints
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/webjars/**",
                    
                    // Fallback endpoints
                    "/fallback/**")
                .permitAll()
                
                // All other requests are permitted to pass through
                // JwtAuthenticationFilter will handle authentication
                .anyExchange().permitAll())
            
            // Build the security filter chain
            .build();
    }
}
```

**⚠️ Lưu ý quan trọng:**
- **KHÔNG** enable OAuth2 Resource Server trong SecurityConfig
- Tất cả requests được `permitAll()` để Gateway filters xử lý
- Authentication được thực hiện bởi `JwtAuthenticationFilter` (GlobalFilter)

### 2.3. Cấu hình Keycloak

#### 2.3.1. KeycloakConfig Bean

```java
@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfig {
    
    private String serverUrl;  // http://localhost:8080
    private String realm;      // train-ticket
    private String clientId;   // train-ticket-gateway
    
    public String getRealmPublicKeyUrl() {
        return String.format("%s/realms/%s", serverUrl, realm);
    }
    
    public String getJwksUrl() {
        return String.format("%s/realms/%s/protocol/openid-connect/certs", 
            serverUrl, realm);
    }
}
```

#### 2.3.2. application.yml Configuration

```yaml
# Keycloak Configuration
keycloak:
  # Keycloak server URL
  server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
  # Realm name configured in Keycloak
  realm: ${KEYCLOAK_REALM:train-ticket}
  # Client ID configured in Keycloak realm
  client-id: ${KEYCLOAK_CLIENT_ID:train-ticket-gateway}

# OAuth2 Resource Server configuration (NOT USED)
# NOTE: This is configured but NOT actively used in SecurityConfig
# Authentication is handled by custom JwtAuthenticationFilter instead
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/auth/realms/train-ticket
          jwk-set-uri: http://localhost:8080/auth/realms/train-ticket/protocol/openid-connect/certs
```

### 2.4. KeycloakTokenValidator

```java
@Component
@RequiredArgsConstructor
public class KeycloakTokenValidator {
    
    private final KeycloakConfig keycloakConfig;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    /**
     * Validates a Keycloak JWT token
     * 1. Parse token header to get key ID (kid)
     * 2. Fetch public key from Keycloak JWKS endpoint
     * 3. Verify token signature
     * 4. Validate claims (issuer, audience, expiration)
     */
    public boolean validateToken(String token) {
        try {
            // Parse token to get header
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            
            // Decode header to get key ID (kid)
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            String kid = header.get("kid").asText();
            
            // Get public key from Keycloak JWKS endpoint
            PublicKey publicKey = getPublicKey(kid).block();
            if (publicKey == null) {
                return false;
            }
            
            // Verify token signature and parse claims
            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            // Validate issuer
            String issuer = claims.getIssuer();
            String expectedIssuer = keycloakConfig.getRealmPublicKeyUrl();
            if (!issuer.equals(expectedIssuer)) {
                return false;
            }
            
            // Validate audience (client ID)
            String audience = claims.getAudience().stream().findFirst().orElse("");
            if (!audience.equals(keycloakConfig.getClientId())) {
                return false;
            }
            
            // Check expiration
            if (claims.getExpiration().before(new Date())) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating Keycloak token", e);
            return false;
        }
    }
    
    /**
     * Extracts username from token (preferred_username claim)
     */
    public String extractUsername(String token) {
        Map<String, Object> claims = parseTokenPayload(token);
        return (String) claims.get("preferred_username");
    }
    
    /**
     * Extracts user ID from token (sub claim)
     */
    public String extractUserId(String token) {
        Map<String, Object> claims = parseTokenPayload(token);
        return (String) claims.get("sub");
    }
    
    /**
     * Extracts roles from token
     * Keycloak roles are in:
     * - realm_access.roles (realm roles)
     * - resource_access.{clientId}.roles (client roles)
     */
    public Set<String> extractRoles(String token) {
        Set<String> roles = new HashSet<>();
        Map<String, Object> claims = parseTokenPayload(token);
        
        // Extract realm roles
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
        if (realmAccess != null) {
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                roles.addAll(realmRoles);
            }
        }
        
        // Extract client-specific roles
        Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");
        if (resourceAccess != null) {
            Map<String, Object> clientAccess = (Map<String, Object>) 
                resourceAccess.get(keycloakConfig.getClientId());
            if (clientAccess != null) {
                List<String> clientRoles = (List<String>) clientAccess.get("roles");
                if (clientRoles != null) {
                    roles.addAll(clientRoles);
                }
            }
        }
        
        return roles;
    }
}
```

### 2.5. JwtAuthenticationFilter (GlobalFilter)

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    
    private final KeycloakTokenValidator keycloakTokenValidator;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // Skip authentication for excluded paths
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }
        
        // Extract token from Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onUnauthorized(exchange, "Missing or invalid authorization token");
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        
        try {
            // Validate token
            if (!keycloakTokenValidator.validateToken(token)) {
                return onUnauthorized(exchange, "Token is invalid or expired");
            }
            
            // Extract user information
            String username = keycloakTokenValidator.extractUsername(token);
            String userId = keycloakTokenValidator.extractUserId(token);
            Set<String> roles = keycloakTokenValidator.extractRoles(token);
            
            // Check admin access for restricted paths
            boolean hasAdminRole = roles.stream()
                .anyMatch(role -> role.equals("ADMIN") || role.endsWith(":ADMIN"));
            
            if (isAdminPath(path) && !hasAdminRole) {
                return onForbidden(exchange, "Access denied. Admin role required.");
            }
            
            // Add user context AND token to headers for downstream services
            // Services can verify token signature if needed (hybrid approach)
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Auth-Token", token)           // Forward token for verification
                .header("X-User-Id", userId != null ? userId : "")
                .header("X-User-Name", username != null ? username : "")
                .header("X-User-Roles", String.join(",", roles))
                .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
            
        } catch (Exception e) {
            logger.error("Error processing Keycloak token", e);
            return onUnauthorized(exchange, "Error processing authentication token");
        }
    }
    
    @Override
    public int getOrder() {
        return -100; // Run after LoggingFilter and RateLimitFilter
    }
    
    private boolean isExcludedPath(String path) {
        return path.startsWith("/api/users/register") ||
               path.startsWith("/api/users/login") ||
               path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
    
    private boolean isAdminPath(String path) {
        return path.contains("/admin/") || path.contains("/management/");
    }
    
    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String body = String.format("{\"success\":false,\"message\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
    
    private Mono<Void> onForbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String body = String.format("{\"success\":false,\"message\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
}
```

**Luồng xác thực:**
```
1. Request đến với header: Authorization: Bearer <jwt-token>
                │
                ▼
2. JwtAuthenticationFilter kiểm tra path có cần auth không?
                │
                ▼
3. Extract token từ Authorization header
                │
                ▼
4. KeycloakTokenValidator.validateToken(token)
   - Parse header để lấy kid
   - Fetch public key từ Keycloak JWKS endpoint
   - Verify signature
   - Validate issuer, audience, expiration
                │
                ▼
5. Extract user info (username, userId, roles)
                │
                ▼
6. Check role-based access (nếu là admin path)
                │
                ▼
7. Add headers: X-Auth-Token, X-User-Id, X-User-Name, X-User-Roles
                │
                ▼
8. Forward request đến downstream service
```

### 2.6. Khác biệt giữa WebFlux Security và MVC Security

| Aspect | Spring MVC | Spring WebFlux |
|--------|------------|----------------|
| Annotation | `@EnableWebSecurity` | `@EnableWebFluxSecurity` |
| Security Config | `HttpSecurity` | `ServerHttpSecurity` |
| Authorization | `authorizeRequests()` | `authorizeExchange()` |
| Path Matching | `antMatchers()` | `pathMatchers()` |
| Filter Chain | `FilterChain` | `SecurityWebFilterChain` |

### 2.7. Thứ tự Filter trong WebFlux Gateway

```
Request
  │
  ├─> Spring Security Filters (SecurityWebFilterChain)
  │   ├─> CORS Filter
  │   ├─> CSRF Filter (disabled)
  │   └─> Authorization Filter
  │
  ├─> Gateway Global Filters (Ordered)
  │   ├─> LoggingFilter (Order: -200)
  │   ├─> RateLimitFilter (Order: -150)
  │   └─> JwtAuthenticationFilter (Order: -100)
  │
  ├─> Route Filters
  │   ├─> StripPrefix
  │   └─> CircuitBreaker
  │
  └─> Backend Service
```

## 3. Global Filters

### 3.1. LoggingFilter

```java
@Component
public class LoggingFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Log request
        return chain.filter(exchange)
            .then(Mono.fromRunnable(() -> {
                // Log response
            }));
    }
    
    @Override
    public int getOrder() {
        return -200; // Chạy đầu tiên
    }
}
```

### 3.2. RateLimitFilter

```java
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Check rate limit
        if (rateLimitExceeded) {
            return onRateLimitExceeded(exchange);
        }
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -150;
    }
}
```

### 3.3. JwtAuthenticationFilter

```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Validate JWT token
        if (!isValidToken(token)) {
            return onUnauthorized(exchange);
        }
        // Add user info to headers
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -100;
    }
}
```

## 4. Best Practices

### 4.1. CORS Configuration

**⚠️ QUAN TRỌNG: Tại sao cần cấu hình CORS ở cả 2 nơi?**

**Lý do thực tế:**
1. **SecurityConfig** - Xử lý CORS ở Spring Security layer (TRƯỚC Gateway filters)
   - Xử lý OPTIONS preflight requests
   - Áp dụng cho tất cả requests đi qua Security
   - **Đây là layer chính xử lý CORS**

2. **application.yml globalcors** - Xử lý CORS ở Gateway layer (SAU Security)
   - **Backup mechanism** cho OPTIONS requests không match route predicates
   - Cần `add-to-simple-url-handler-mapping: true` để hoạt động đúng
   - Hữu ích khi có requests không đi qua Security filters

**Khi nào có thể bỏ globalcors?**
- Nếu SecurityConfig đã xử lý CORS đầy đủ và bạn chắc chắn tất cả requests đều đi qua Security
- Nếu không có vấn đề với OPTIONS preflight requests

**Khi nào nên giữ cả 2?**
- Khi muốn có backup mechanism
- Khi có requests không đi qua Security (hiếm)
- Khi muốn đảm bảo 100% CORS được xử lý

**Khuyến nghị:**
- **Giữ cả 2** để đảm bảo CORS hoạt động trong mọi trường hợp
- Đảm bảo `add-to-simple-url-handler-mapping: true` trong globalcors

- **⚠️ QUAN TRỌNG: Trong Spring Cloud 2025.0.0+, globalcors PHẢI ở trong server.webflux:**
  ```yaml
  spring:
    cloud:
      gateway:
        server:
          webflux:
            globalcors:
              # QUAN TRỌNG: Enable này để xử lý OPTIONS requests không match routes
              add-to-simple-url-handler-mapping: true
              cors-configurations:  # ✅ kebab-case
                '[/**]':
                  allowedOrigins:
                    - "http://localhost:4200"
                    - "http://localhost:3000"
                  allowedMethods:
                    - GET
                    - POST
                    - PUT
                    - DELETE
                    - PATCH
                    - OPTIONS
                  allowedHeaders: "*"
                  allowCredentials: true
                  maxAge: 3600
  ```

- **Không dùng `*` với `allowCredentials: true`:**
  ```yaml
  # ❌ SAI
  allowedOrigins: "*"
  allowCredentials: true
  
  # ✅ ĐÚNG
  allowedOrigins:
    - "http://localhost:4200"
  allowCredentials: true
  ```

- **Property name:**
  ```yaml
  # ❌ SAI - camelCase
  corsConfigurations:
  
  # ✅ ĐÚNG - kebab-case
  cors-configurations:
  ```

### 4.2. Security Configuration với Keycloak

- **Disable CSRF cho stateless API:**
  ```java
  .csrf(ServerHttpSecurity.CsrfSpec::disable)
  ```

- **Disable HTTP Basic và Form Login:**
  ```java
  .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
  .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
  ```

- **Permit all requests để Gateway filters xử lý:**
  ```java
  .anyExchange().permitAll()
  ```

- **KHÔNG enable OAuth2 Resource Server:**
  ```java
  // ❌ KHÔNG làm thế này
  .oauth2ResourceServer(oauth2 -> oauth2.jwt(...))
  
  // ✅ Đúng - để JwtAuthenticationFilter xử lý
  .anyExchange().permitAll()
  ```

- **Hybrid Approach - Forward token đến services:**
  - Gateway validate token signature với Keycloak
  - Gateway extract user context (userId, roles)
  - Gateway forward token trong header `X-Auth-Token`
  - Downstream services có thể verify token nếu cần

- **Keycloak Configuration Best Practices:**
  ```yaml
  keycloak:
    server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
    realm: ${KEYCLOAK_REALM:train-ticket}
    client-id: ${KEYCLOAK_CLIENT_ID:train-ticket-gateway}
  ```
  
  - Sử dụng environment variables cho production
  - Client trong Keycloak nên là **public client** hoặc **confidential client**
  - Đảm bảo JWKS endpoint accessible từ Gateway

### 4.3. Error Handling

- **GlobalErrorFilter** xử lý tất cả exceptions:
  ```java
  @Component
  @Order(-1)  // Chạy cuối cùng
  public class GlobalErrorFilter implements ErrorWebExceptionHandler {
      // Handle all exceptions
  }
  ```

## 5. Testing

### 5.1. Kiểm tra Gateway hoạt động

```bash
# Health check
curl http://localhost:8189/actuator/health

# Test route
curl http://localhost:8189/api/users/health
```

### 5.2. Kiểm tra CORS

```bash
# Preflight request
curl -X OPTIONS http://localhost:8189/api/users/health \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: GET" \
  -v
```

### 5.3. Kiểm tra Keycloak Authentication

```bash
# 1. Lấy token từ Keycloak
TOKEN=$(curl -X POST http://localhost:8080/auth/realms/train-ticket/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user" \
  -d "password=password" \
  -d "grant_type=password" \
  -d "client_id=train-ticket-gateway" \
  | jq -r '.access_token')

# 2. Test authenticated request
curl http://localhost:8189/api/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -v

# 3. Kiểm tra headers được forward
# Response sẽ có headers: X-User-Id, X-User-Name, X-User-Roles
```

### 5.4. Kiểm tra Metrics

```bash
# Xem số lượng routes
curl http://localhost:8189/actuator/metrics/spring.cloud.gateway.routes.count
```

## 6. Troubleshooting

### 6.1. Gateway không nhận routes

**Vấn đề:** Routes không được load

**Giải pháp:**
- **Spring Cloud 2025.0.0+**: Dùng `spring.cloud.gateway.server.webflux.routes`
- **Spring Cloud < 2025.0.0**: Dùng `spring.cloud.gateway.routes`
- Kiểm tra Eureka service discovery hoạt động
- Xem logs: `spring.cloud.gateway.routes.count` metric
- Kiểm tra deprecation warnings trong logs

### 6.2. CORS không hoạt động

**Vấn đề:** Browser báo CORS error

**Giải pháp:**
- Kiểm tra CORS config trong `SecurityConfig`
- Kiểm tra `globalcors` trong `application.yml`
- Đảm bảo `allowCredentials: true` không dùng với `*` origin

### 6.3. Security filter không chạy

**Vấn đề:** Security filters không được áp dụng

**Giải pháp:**
- Đảm bảo dùng `@EnableWebFluxSecurity` (không phải `@EnableWebSecurity`)
- Kiểm tra `SecurityWebFilterChain` được tạo đúng
- Xem thứ tự filter với `@Order`

### 6.4. Keycloak token validation fails

**Vấn đề:** Token validation luôn fail

**Giải pháp:**
- Kiểm tra Keycloak server đang chạy và accessible
- Verify JWKS endpoint: `curl http://localhost:8080/auth/realms/train-ticket/protocol/openid-connect/certs`
- Kiểm tra `keycloak.server-url`, `keycloak.realm`, `keycloak.client-id` trong `application.yml`
- Verify token issuer và audience match với Keycloak config
- Check logs để xem lỗi cụ thể (signature, expiration, issuer mismatch)

### 6.5. Token không được forward đến services

**Vấn đề:** Downstream services không nhận được token

**Giải pháp:**
- Kiểm tra `JwtAuthenticationFilter` có add header `X-Auth-Token`
- Verify filter order: JwtAuthenticationFilter phải chạy trước routing
- Check logs để xem token có được extract đúng không
- Test với curl để xem headers được forward:
  ```bash
  curl http://localhost:8189/api/tickets \
    -H "Authorization: Bearer $TOKEN" \
    -v
  ```

## 7. Migration từ Spring Cloud < 2025.0.0

### 7.1. Thay đổi Property Prefixes

Nếu bạn đang migrate từ phiên bản cũ hơn:

**Bước 1:** Cập nhật `application.yml`:
```yaml
# CŨ (deprecated)
spring:
  cloud:
    gateway:
      routes: [...]

# MỚI (Spring Cloud 2025.0.0+)
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes: [...]
```

**Bước 2:** (Tùy chọn) Sử dụng Spring Boot Properties Migrator:
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-properties-migrator'
}
```

### 7.2. Các Property Khác Cần Migrate

| Deprecated | New |
|------------|-----|
| `spring.cloud.gateway.metrics.*` | `spring.cloud.gateway.server.webflux.metrics.*` |
| `spring.cloud.gateway.globalcors.*` | `spring.cloud.gateway.server.webflux.globalcors.*` |

**⚠️ Lưu ý quan trọng về globalcors:**
- Trong Spring Cloud 2025.0.0+, `globalcors` **PHẢI** được đặt trong `server.webflux`
- Property name là `cors-configurations` (kebab-case), không phải `corsConfigurations` (camelCase)
- Cấu hình đúng:
  ```yaml
  spring:
    cloud:
      gateway:
        server:
          webflux:
            globalcors:
              cors-configurations:  # ✅ kebab-case
                '[/**]':
                  allowedOrigins: [...]
  ```

## 8. Tài liệu tham khảo

- [Spring Cloud Gateway Documentation](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Spring Cloud 2025.0 Release Notes](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2025.0-Release-Notes)
- [Spring Security WebFlux](https://docs.spring.io/spring-security/reference/reactive/index.html)
- [WebFlux Reactive Programming](https://docs.spring.io/spring-framework/reference/web/webflux.html)
