# Security Configuration Guide - API Gateway

## 📋 Tổng quan

Hướng dẫn này tổng hợp **TẤT CẢ** cấu hình security cần thiết cho API Gateway, giải thích rõ ràng **CẦN cấu hình gì** và **Ở ĐÂU**.

---

## 🎯 Nguyên tắc cấu hình

### Single Source of Truth
- **Mỗi cấu hình chỉ có 1 nơi duy nhất** quyết định
- **Không trùng lặp** giữa các file
- **Rõ ràng** về trách nhiệm của từng component

### Architecture
```
Spring Security WebFlux (CORS only)
    ↓
Gateway Global Filters
    ├─ LoggingFilter
    ├─ RateLimitFilter
    └─ JwtAuthenticationFilter ← Authentication logic
    ↓
Route to Microservice
```

---

## 📝 Cấu hình cần thiết

### 1. ✅ SecurityConfig.java - Spring Security WebFlux

**File:** `apigateway/src/main/java/com/example/apigateway/config/SecurityConfig.java`

**Mục đích:** Cấu hình Spring Security WebFlux layer (chỉ xử lý CORS)

**CẦN cấu hình:**

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // 1. CORS Configuration Source
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://localhost:3000"));
        
        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Allowed headers
        configuration.setAllowedHeaders(List.of("*"));
        
        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-User-Id",
            "X-User-Name",
            "X-User-Roles",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset"));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Max age
        configuration.setMaxAge(3600L);
        
        // Apply to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    // 2. Security Web Filter Chain
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Disable unused security features
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            
            // Authorization rules
            .authorizeExchange(ex -> ex
                // Allow OPTIONS requests (CORS preflight)
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // All other requests pass through to Gateway filters
                // Authentication is handled by JwtAuthenticationFilter
                .anyExchange().permitAll())
            
            .build();
    }
}
```

**KHÔNG cần:**
- ❌ `.pathMatchers(...).permitAll()` cho public endpoints (vì `.anyExchange().permitAll()` đã cho phép tất cả)
- ❌ OAuth2 Resource Server configuration (dùng custom JWT filter)

**Lý do:**
- Spring Security chỉ xử lý CORS, không validate JWT token
- Authentication thực sự được xử lý bởi `JwtAuthenticationFilter`
- `.anyExchange().permitAll()` cho phép tất cả requests pass through

---

### 2. ✅ JwtAuthenticationFilter.java - Authentication Logic

**File:** `apigateway/src/main/java/com/example/apigateway/filter/JwtAuthenticationFilter.java`

**Mục đích:** Validate JWT token và extract user info (SINGLE SOURCE OF TRUTH cho public endpoints)

**CẦN cấu hình:**

```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final KeycloakTokenValidator tokenValidator;

    // ✅ SINGLE SOURCE OF TRUTH - Public endpoints
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
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
        
        // Actuator endpoints
        "/actuator",
        
        // OpenAPI/Swagger endpoints
        "/swagger-ui.html",
        "/v3/api-docs",
        
        // Fallback endpoints
        "/fallback"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // Skip authentication for public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }
        
        // Extract and validate token
        String authHeader = exchange.getRequest().getHeaders()
            .getFirst(Constants.AUTHORIZATION_HEADER);
        
        if (authHeader == null || !authHeader.startsWith(Constants.BEARER_PREFIX)) {
            return onUnauthorized(exchange, "Missing or invalid authorization token");
        }
        
        String token = authHeader.substring(Constants.BEARER_PREFIX.length());
        
        // Validate token with Keycloak
        return tokenValidator.validateToken(token)
            .flatMap(claims -> {
                // Extract user info
                String userId = extractUserId(claims);
                String username = extractUsername(claims);
                List<String> roles = extractRoles(claims);
                
                // Add headers
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header(Constants.USER_ID_HEADER, userId)
                    .header(Constants.USER_NAME_HEADER, username)
                    .header(Constants.USER_ROLES_HEADER, String.join(",", roles))
                    .header(Constants.AUTH_TOKEN_HEADER, token)
                    .build();
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            })
            .switchIfEmpty(Mono.defer(() -> 
                onUnauthorized(exchange, "Token is invalid or expired")));
    }

    @Override
    public int getOrder() {
        return -100; // After LoggingFilter (-200) and RateLimitFilter (-150)
    }
}
```

**CẦN maintain:**
- ✅ `PUBLIC_PATHS` list - thêm/sửa public endpoints ở đây
- ✅ `extractUserId()`, `extractUsername()`, `extractRoles()` - nếu JWT structure thay đổi

**KHÔNG cần:**
- ❌ Cấu hình ở SecurityConfig (đã xử lý ở đây)

---

### 3. ✅ KeycloakTokenValidator.java - Token Validation

**File:** `apigateway/src/main/java/com/example/apigateway/util/KeycloakTokenValidator.java`

**Mục đích:** Validate JWT token với Keycloak (signature, issuer, audience, expiration)

**CẦN cấu hình:**

```java
@Component
public class KeycloakTokenValidator {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final KeycloakConfig keycloakConfig;
    
    // Cache for public keys
    private final ConcurrentMap<String, PublicKey> publicKeyCache = new ConcurrentHashMap<>();
    
    public Mono<Claims> validateToken(String token) {
        // 1. Parse token header to get kid
        // 2. Get public key from cache or fetch from Keycloak JWKS
        // 3. Verify token signature
        // 4. Validate issuer, audience, expiration
        // 5. Return claims if valid
    }
}
```

**CẦN maintain:**
- ✅ Keycloak JWKS URL (tự động từ `KeycloakConfig`)
- ✅ Public key cache (tự động cache, có thể clear nếu cần)

**KHÔNG cần:**
- ❌ Manual configuration (tự động từ `KeycloakConfig`)

---

### 4. ✅ KeycloakConfig.java - Keycloak Properties

**File:** `apigateway/src/main/java/com/example/apigateway/config/KeycloakConfig.java`

**Mục đích:** Map Keycloak properties từ `application.yml`

**CẦN cấu hình:**

```java
@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfig {
    private String serverUrl;
    private String realm;
    private String clientId;
    
    public String getRealmPublicKeyUrl() {
        return String.format("%s/realms/%s", serverUrl, realm);
    }
    
    public String getJwksUrl() {
        return String.format("%s/realms/%s/protocol/openid-connect/certs", 
            serverUrl, realm);
    }
}
```

**KHÔNG cần:**
- ❌ Manual configuration (tự động từ `application.yml`)

---

### 5. ✅ application.yml - Keycloak Configuration

**File:** `apigateway/src/main/resources/application.yml`

**Mục đích:** Cấu hình Keycloak server URL, realm, client ID

**CẦN cấu hình:**

```yaml
keycloak:
  # Keycloak server URL
  server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
  
  # Realm name
  realm: ${KEYCLOAK_REALM:train-ticket}
  
  # Client ID
  client-id: ${KEYCLOAK_CLIENT_ID:train-ticket-gateway}
```

**CẦN maintain:**
- ✅ Keycloak server URL (nếu thay đổi)
- ✅ Realm name (nếu thay đổi)
- ✅ Client ID (nếu thay đổi)

**KHÔNG cần:**
- ❌ OAuth2 Resource Server configuration (không dùng)

---

### 6. ✅ Constants.java - Header Constants

**File:** `apigateway/src/main/java/com/example/apigateway/config/Constants.java`

**Mục đích:** Định nghĩa constants cho headers

**CẦN có:**

```java
public final class Constants {
    // Correlation ID
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    
    // User info headers
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_NAME_HEADER = "X-User-Name";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    
    // Auth token header
    public static final String AUTH_TOKEN_HEADER = "X-Auth-Token";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    
    // Rate limit headers
    public static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    
    // Other
    public static final String UNKNOWN = "unknown";
}
```

**CẦN maintain:**
- ✅ Thêm header constants mới nếu cần

---

## 📊 Tổng hợp: Cấu hình ở đâu?

| Cấu hình | File | Mục đích | Cần maintain? |
|----------|------|----------|---------------|
| **CORS** | `SecurityConfig.java` | Cấu hình CORS headers | ✅ Có (nếu thêm origin mới) |
| **Public Endpoints** | `JwtAuthenticationFilter.java` | Danh sách endpoints không cần auth | ✅ **CÓ** - Single source of truth |
| **Keycloak Config** | `application.yml` | Keycloak server URL, realm, client ID | ✅ Có (nếu thay đổi Keycloak) |
| **Token Validation** | `KeycloakTokenValidator.java` | Logic validate token | ❌ Không (tự động) |
| **User Info Extraction** | `JwtAuthenticationFilter.java` | Extract userId, username, roles | ⚠️ Có (nếu JWT structure thay đổi) |
| **Header Constants** | `Constants.java` | Định nghĩa header names | ⚠️ Có (nếu thêm header mới) |

---

## ✅ Checklist cấu hình

### Bắt buộc (Required)

- [x] **SecurityConfig.java**
  - [x] CORS configuration
  - [x] `.anyExchange().permitAll()` để cho phép tất cả requests pass through
  - [x] Disable CSRF, HTTP Basic, Form Login, Logout

- [x] **JwtAuthenticationFilter.java**
  - [x] `PUBLIC_PATHS` list - định nghĩa public endpoints
  - [x] Token extraction và validation logic
  - [x] User info extraction (userId, username, roles)
  - [x] Header forwarding (X-User-Id, X-User-Name, X-User-Roles, X-Auth-Token)

- [x] **KeycloakTokenValidator.java**
  - [x] Token validation logic
  - [x] Public key caching

- [x] **KeycloakConfig.java**
  - [x] Map properties từ `application.yml`

- [x] **application.yml**
  - [x] Keycloak server URL
  - [x] Realm name
  - [x] Client ID

- [x] **Constants.java**
  - [x] Header constants

### Không cần (Not Required)

- [ ] ❌ OAuth2 Resource Server configuration trong `application.yml`
- [ ] ❌ `.pathMatchers(...).permitAll()` trong `SecurityConfig.java` (vì `.anyExchange().permitAll()` đã đủ)
- [ ] ❌ Manual public key management (tự động fetch từ Keycloak)

---

## 🔄 Flow cấu hình

```
1. application.yml
   └─> KeycloakConfig (map properties)
       └─> KeycloakTokenValidator (dùng để validate)

2. SecurityConfig
   └─> CORS configuration
   └─> .anyExchange().permitAll() (cho phép tất cả pass through)

3. JwtAuthenticationFilter
   └─> PUBLIC_PATHS (single source of truth)
   └─> KeycloakTokenValidator.validateToken()
   └─> Extract user info và add headers

4. Constants
   └─> Header names được sử dụng bởi filters
```

---

## 🎯 Kết luận

### Cấu hình tối thiểu cần thiết:

1. **SecurityConfig.java** - Chỉ CORS và `.anyExchange().permitAll()`
2. **JwtAuthenticationFilter.java** - `PUBLIC_PATHS` list và authentication logic
3. **KeycloakTokenValidator.java** - Token validation logic
4. **KeycloakConfig.java** - Map properties
5. **application.yml** - Keycloak configuration
6. **Constants.java** - Header constants

### Single Source of Truth:

- **Public Endpoints:** `JwtAuthenticationFilter.PUBLIC_PATHS` ← **CHỈ CẦN maintain ở đây**
- **CORS:** `SecurityConfig.corsConfigurationSource()`
- **Keycloak Config:** `application.yml` → `KeycloakConfig`

### Không cần:

- ❌ `.pathMatchers(...).permitAll()` trong SecurityConfig
- ❌ OAuth2 Resource Server configuration
- ❌ Duplicate public endpoints list

---

## 📝 Maintenance Guide

### Khi thêm public endpoint mới:

1. **CHỈ cần** thêm vào `JwtAuthenticationFilter.PUBLIC_PATHS`
2. **KHÔNG cần** thêm vào SecurityConfig

### Khi thay đổi Keycloak:

1. **CHỈ cần** update `application.yml`
2. **KHÔNG cần** thay đổi code

### Khi thay đổi CORS:

1. **CHỈ cần** update `SecurityConfig.corsConfigurationSource()`

---

## ✅ Best Practices

1. ✅ **Single Source of Truth** - Mỗi cấu hình chỉ có 1 nơi
2. ✅ **Separation of Concerns** - Mỗi component có trách nhiệm rõ ràng
3. ✅ **Minimal Configuration** - Chỉ cấu hình những gì cần thiết
4. ✅ **Clear Documentation** - Comments rõ ràng về mục đích
