Tôi sẽ đọc skill file trước, sau đó khám phá chi tiết cấu trúc API Gateway để giải thích cho bạn.

[15 tools called]

Tôi đã đọc toàn bộ code của API Gateway. Đây là giải thích chi tiết từng yếu tố kỹ thuật dành cho người mới học.

---

# 📚 GIẢI THÍCH CHI TIẾT API GATEWAY

## 1. API GATEWAY LÀ GÌ?

**API Gateway** giống như "**cổng chính**" của một tòa nhà lớn (hệ thống microservices). Thay vì để khách (client) vào từng phòng (service) riêng lẻ, tất cả đều phải đi qua cổng chính này.

```
     ┌─────────────────┐
     │    Client       │ (Web, Mobile App)
     └────────┬────────┘
              │
              ▼
     ┌─────────────────┐
     │  API Gateway    │ ◄── Điểm vào duy nhất
     │   (Port 8080)   │
     └────────┬────────┘
              │
    ┌─────────┼─────────┬─────────────┐
    ▼         ▼         ▼             ▼
┌───────┐ ┌───────┐ ┌─────────┐ ┌──────────┐
│ User  │ │Ticket │ │Inventory│ │ Payment  │
│Service│ │Service│ │ Service │ │ Service  │
└───────┘ └───────┘ └─────────┘ └──────────┘
```

---

## 2. CẤU TRÚC DỰ ÁN

```
apigateway/
├── build.gradle                    # Quản lý dependencies
├── Dockerfile                      # Container hóa
├── src/main/
│   ├── java/com/example/apigateway/
│   │   ├── ApigatewayApplication.java    # Entry point
│   │   ├── config/                       # Cấu hình
│   │   │   ├── Constants.java
│   │   │   ├── GatewayConfig.java
│   │   │   └── KeycloakConfig.java
│   │   ├── controller/
│   │   │   └── FallbackController.java   # Xử lý khi service lỗi
│   │   ├── filter/                       # Các bộ lọc
│   │   │   ├── GlobalErrorFilter.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── LoggingFilter.java
│   │   │   └── RateLimitFilter.java
│   │   └── util/
│   │       ├── JwtUtil.java
│   │       └── KeycloakTokenValidator.java
│   └── resources/
│       └── application.yml               # Cấu hình chính
```

---

## 3. DEPENDENCIES (build.gradle)

```15:49:apigateway/build.gradle
java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

ext {
	set('springCloudVersion', "2025.0.1")
}

dependencies {
	implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
	
	// Resilience4j for circuit breaker and rate limiting
	implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
	implementation 'io.github.resilience4j:resilience4j-reactor:2.1.0'
	implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j'
	
	// Keycloak for authentication and authorization
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
	
	// JWT (still needed for token parsing)
	implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
	implementation 'io.jsonwebtoken:jjwt-impl:0.12.3'
	implementation 'io.jsonwebtoken:jjwt-jackson:0.12.3'
	
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

	// Lombok
	compileOnly 'org.projectlombok:lombok'
	annotationProcessor 'org.projectlombok:lombok'
}
```

**Giải thích từng dependency:**

| Dependency | Mục đích |
|------------|----------|
| `spring-cloud-starter-gateway` | Framework chính cho API Gateway (reactive, non-blocking) |
| `spring-cloud-starter-netflix-eureka-client` | Kết nối với Eureka để **Service Discovery** (tự tìm kiếm service) |
| `spring-boot-starter-actuator` | Monitoring và health check |
| `spring-boot-starter-data-redis-reactive` | Kết nối Redis cho rate limiting |
| `resilience4j-*` | **Circuit Breaker** - ngắt mạch khi service lỗi |
| `spring-boot-starter-oauth2-resource-server` | Hỗ trợ OAuth2/Keycloak |
| `jjwt-*` | Thư viện xử lý JWT token |
| `lombok` | Giảm code boilerplate (tự sinh getter/setter) |

---

## 4. APPLICATION.YML - CẤU HÌNH CHÍNH

### 4.1. Cấu hình cơ bản

```yaml
spring:
  application:
    name: apigateway      # Tên service đăng ký với Eureka
  main:
    web-application-type: reactive  # Bắt buộc dùng WebFlux (reactive)
```

**Tại sao dùng Reactive?**
- Spring Cloud Gateway được xây dựng trên **WebFlux** (non-blocking I/O)
- Xử lý được nhiều request đồng thời hơn servlet truyền thống
- Phù hợp với vai trò của Gateway là xử lý lượng traffic lớn

### 4.2. Redis Configuration

```yaml
data:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
```

**Redis dùng để làm gì?**
- Lưu trữ thông tin **rate limiting** (giới hạn request)
- Cache data tạm thời
- Trong môi trường production với nhiều instance Gateway, Redis giúp đồng bộ rate limit

### 4.3. ROUTING - Định tuyến Request

```66:78:apigateway/src/main/resources/application.yml
      routes:
        # Route: User Service
        # Matches: /api/users/** -> forwards to user-service
        - id: user-service
          uri: lb://user-service # lb:// = load balance via Eureka service discovery
          predicates:
            - Path=/api/users/** # Match all paths starting with /api/users/
          filters:
            - StripPrefix=1 # Remove /api prefix before forwarding
            - name: CircuitBreaker # Enable circuit breaker for fault tolerance
              args:
                name: user-service-circuit-breaker
                fallbackUri: forward:/fallback/user-service # Fallback when service is down
```

**Giải thích:**

| Thuộc tính | Ý nghĩa |
|------------|---------|
| `id` | Định danh duy nhất cho route |
| `uri: lb://user-service` | `lb://` = Load Balanced. Gateway sẽ hỏi Eureka: "user-service ở đâu?" rồi tự động phân phối request |
| `predicates` | Điều kiện để khớp route. `Path=/api/users/**` = bất kỳ URL nào bắt đầu bằng `/api/users/` |
| `filters` | Các xử lý trước khi forward |
| `StripPrefix=1` | Bỏ phần đầu tiên của path. `/api/users/login` → `/users/login` |
| `CircuitBreaker` | Nếu service lỗi quá nhiều, chuyển sang fallback |

**Luồng hoạt động:**

```
Client: GET /api/users/profile
         │
         ▼
    ┌─────────────┐
    │ API Gateway │
    │  Path match │──► Khớp với route "user-service"
    │  StripPrefix│──► Bỏ "/api" → "/users/profile"
    │  lb://      │──► Hỏi Eureka → user-service đang ở localhost:8081
    └─────────────┘
         │
         ▼
    User-Service nhận: GET /users/profile
```

### 4.4. CORS Configuration

```yaml
globalcors:
  cors-configurations:
    "[/**]":
      allowedOrigins: "*"
      allowedMethods:
        - GET
        - POST
        - PUT
        - DELETE
      allowedHeaders: "*"
      allowCredentials: true
```

**CORS là gì?**
- **Cross-Origin Resource Sharing** - Cho phép web từ domain khác gọi API
- Ví dụ: Frontend ở `http://localhost:3000` gọi API ở `http://localhost:8080`
- Browser mặc định chặn điều này, CORS config cho phép nó

### 4.5. Circuit Breaker Configuration (Resilience4j)

```154:166:apigateway/src/main/resources/application.yml
resilience4j:
  # Circuit Breaker: Prevents cascading failures
  # Opens circuit when service fails too often, returns fallback response
  circuitbreaker:
    configs:
      # Default configuration for all circuit breakers
      default:
        slidingWindowSize: 10 # Number of calls to track
        minimumNumberOfCalls: 5 # Minimum calls before circuit can open
        failureRateThreshold: 50 # Open circuit if 50% of calls fail
        waitDurationInOpenState: 10s # Wait 10s before trying again
        permittedNumberOfCallsInHalfOpenState: 3 # Test with 3 calls when half-open
```

**Circuit Breaker là gì?** (Giống cầu dao điện)

```
Trạng thái: CLOSED (Bình thường)
         │
         │  50% request fail trong 10 calls gần nhất
         ▼
    OPEN (Mở cầu dao - ngắt kết nối)
    → Tất cả request trả về fallback ngay lập tức
    → Không gọi service đang lỗi
         │
         │  Sau 10 giây
         ▼
    HALF-OPEN (Thử lại)
    → Cho 3 request thử gọi service
    → Nếu thành công → CLOSED
    → Nếu fail → OPEN
```

**Tại sao cần Circuit Breaker?**
- Ngăn **cascading failure** (lỗi lan truyền)
- Nếu Payment Service chết, không muốn tất cả request đợi timeout
- Trả về fallback nhanh chóng, bảo vệ hệ thống

---

## 5. FILTERS - CÁC BỘ LỌC

### 5.1. Thứ tự thực thi Filter (Order)

```
Request vào →  LoggingFilter (-200)
                    │
                    ▼
             RateLimitFilter (-150)
                    │
                    ▼
             JwtAuthenticationFilter (-100)
                    │
                    ▼
               Routing & Forward
                    │
Response ra ← GlobalErrorFilter (-1)
```

**Order càng nhỏ (âm lớn) → chạy trước**

### 5.2. LoggingFilter - Ghi Log

```31:84:apigateway/src/main/java/com/example/apigateway/filter/LoggingFilter.java
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate or extract correlation ID
        String correlationId = request.getHeaders().getFirst(Constants.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Record start time
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(Constants.START_TIME_ATTRIBUTE, startTime);

        // Extract request details
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeaders().getFirst(Constants.USER_AGENT_HEADER);
        String userId = request.getHeaders().getFirst(Constants.USER_ID_HEADER);

        // Log incoming request
        logger.info("Incoming request: {} {} | Client: {} | User: {} | Correlation: {} | User-Agent: {}",
                method, path, clientIp, userId != null ? userId : "anonymous", correlationId, userAgent);

        // Add correlation ID to request headers
        final String finalCorrelationId = correlationId;
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(Constants.CORRELATION_ID_HEADER, finalCorrelationId)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    // Log outgoing response
                    ServerHttpResponse response = exchange.getResponse();
                    Long startTimeValue = exchange.getAttribute(Constants.START_TIME_ATTRIBUTE);
                    long duration = startTimeValue != null
                            ? System.currentTimeMillis() - startTimeValue
                            : 0;

                    int statusCode = response.getStatusCode() != null
                            ? response.getStatusCode().value()
                            : 0;

                    if (statusCode >= 400) {
                        logger.warn(
                                "Response: {} {} | Status: {} | Duration: {}ms | Correlation: {} | Client: {} | User: {} | User-Agent: {}",
                                method, path, statusCode, duration, finalCorrelationId, clientIp, userId, userAgent);
                    } else {
                        logger.info(
                                "Response: {} {} | Status: {} | Duration: {}ms | Correlation: {} | Client: {} | User: {} | User-Agent: {}",
                                method, path, statusCode, duration, finalCorrelationId, clientIp, userId, userAgent);
                    }
                }));
    }
```

**Chức năng:**
1. **Correlation ID**: Tạo ID duy nhất cho mỗi request, theo dõi xuyên suốt các service
2. **Logging**: Ghi log chi tiết (method, path, IP, user, thời gian xử lý)
3. **Timing**: Đo thời gian xử lý request

**Log output ví dụ:**
```
INFO  Incoming request: GET /api/users/profile | Client: 192.168.1.1 | User: user123 | Correlation: abc-123
INFO  Response: GET /api/users/profile | Status: 200 | Duration: 45ms | Correlation: abc-123
```

### 5.3. RateLimitFilter - Giới hạn Request

```45:79:apigateway/src/main/java/com/example/apigateway/filter/RateLimitFilter.java
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Get rate limit key
        String key = getRateLimitKey(request);
        int maxRequests = getMaxRequests(path);

        // Check rate limit
        RateLimitEntry entry = rateLimitMap.compute(key, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || now - v.windowStart.get() > WINDOW_SIZE_MS) {
                return new RateLimitEntry(now);
            }
            return v;
        });

        int currentCount = entry.count.incrementAndGet();

        if (currentCount > maxRequests) {
            logger.warn("Rate limit exceeded for key: {} (count: {}, max: {})", key, currentCount, maxRequests);
            return onRateLimitExceeded(exchange);
        }

        // Add rate limit headers
        ServerHttpResponse response = exchange.getResponse();
        long remaining = Math.max(0, maxRequests - currentCount);
        long resetTime = (entry.windowStart.get() + WINDOW_SIZE_MS - System.currentTimeMillis()) / 1000;

        response.getHeaders().add(Constants.RATE_LIMIT_LIMIT_HEADER, String.valueOf(maxRequests));
        response.getHeaders().add(Constants.RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining));
        response.getHeaders().add(Constants.RATE_LIMIT_RESET_HEADER, String.valueOf(Math.max(0, resetTime)));

        return chain.filter(exchange);
    }
```

**Rate Limiting là gì?**
- Giới hạn số request trong khoảng thời gian
- Bảo vệ server khỏi bị quá tải hoặc tấn công DDoS

**Cấu hình:**
```java
private static final int DEFAULT_REQUESTS_PER_MINUTE = 100;     // 100 request/phút (thông thường)
private static final int AUTH_REQUESTS_PER_MINUTE = 10;         // 10 request/phút (login, register)
```

**Response headers khi bị giới hạn:**
```
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 45
Retry-After: 60
```

### 5.4. JwtAuthenticationFilter - Xác thực

```61:123:apigateway/src/main/java/com/example/apigateway/filter/JwtAuthenticationFilter.java
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();

        logger.debug("Processing request: {} {}", method, path);

        // Skip authentication for excluded paths
        if (isExcludedPath(path)) {
            logger.debug("Skipping authentication for excluded path: {}", path);
            return chain.filter(exchange);
        }

        // Extract and validate token
        String authHeader = request.getHeaders().getFirst(Constants.AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(Constants.BEARER_PREFIX)) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            return onUnauthorized(exchange, "Missing or invalid authorization token");
        }

        String token = authHeader.substring(Constants.BEARER_PREFIX.length());

        try {
            if (!keycloakTokenValidator.validateToken(token)) {
                logger.warn("Invalid or expired Keycloak token for path: {}", path);
                return onUnauthorized(exchange, "Token is invalid or expired");
            }

            // Extract user information from Keycloak token
            String username = keycloakTokenValidator.extractUsername(token);
            String userId = keycloakTokenValidator.extractUserId(token);
            Set<String> roles = keycloakTokenValidator.extractRoles(token);

            logger.debug("Authenticated user: {} (id: {}) for path: {}", username, userId, path);

            // Check admin access for restricted paths
            // Keycloak roles might be prefixed with realm or client name
            boolean hasAdminRole = roles.stream()
                    .anyMatch(role -> role.equals("ADMIN") || role.equals("admin") ||
                            role.endsWith(":ADMIN") || role.endsWith(":admin"));

            if (isAdminPath(path) && !hasAdminRole) {
                logger.warn("Forbidden access attempt by user {} to admin path: {}", username, path);
                return onForbidden(exchange, "Access denied. Admin role required.");
            }

            // Add user context AND token to headers for downstream services
            // Services can verify token signature if needed (hybrid approach)
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header(Constants.AUTH_TOKEN_HEADER, token) // Forward token for verification
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
```

**Luồng xác thực:**

```
1. Request đến với header: Authorization: Bearer <jwt-token>
                │
                ▼
2. Kiểm tra path có cần auth không?
   - /api/users/login → SKIP (không cần)
   - /api/users/profile → CONTINUE
                │
                ▼
3. Validate token với Keycloak
   - Kiểm tra signature
   - Kiểm tra expiration
   - Kiểm tra issuer
                │
                ▼
4. Extract thông tin user
   - username, userId, roles
                │
                ▼
5. Kiểm tra quyền admin (nếu cần)
   - /api/users/roles → Cần role ADMIN
                │
                ▼
6. Thêm headers và forward
   X-User-Id: 123
   X-User-Name: john
   X-User-Roles: USER,ADMIN
```

**Excluded paths (không cần đăng nhập):**
```java
private static final List<String> EXCLUDED_PATHS = List.of(
    "/api/users/register",
    "/api/users/login",
    "/api/users/forgot-password",
    "/actuator"
);
```

**Admin paths (cần role ADMIN):**
```java
private static final List<String> ADMIN_PATHS = List.of(
    "/api/users/roles",
    "/api/inventory/routes",
    "/api/payments/stats"
);
```

### 5.5. GlobalErrorFilter - Xử lý Lỗi

```33:85:apigateway/src/main/java/com/example/apigateway/filter/GlobalErrorFilter.java
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        String path = exchange.getRequest().getURI().getPath();
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);

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
```

**Chức năng:** Bắt tất cả exception và trả về response JSON nhất quán

| Exception | HTTP Status | Error Code |
|-----------|-------------|------------|
| `ConnectException` | 503 | SERVICE_UNAVAILABLE |
| `TimeoutException` | 504 | GATEWAY_TIMEOUT |
| Khác | 500 | INTERNAL_ERROR |

**Response mẫu:**
```json
{
  "success": false,
  "message": "Service is temporarily unavailable",
  "statusCode": 503,
  "errorCode": "SERVICE_UNAVAILABLE",
  "path": "/api/users/profile",
  "correlationId": "abc-123",
  "timestamp": "2026-01-17T10:30:00"
}
```

---

## 6. FALLBACK CONTROLLER

```17:53:apigateway/src/main/java/com/example/apigateway/controller/FallbackController.java
@RestController
public class FallbackController {

    @RequestMapping("/fallback/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        return createFallbackResponse("User service is temporarily unavailable");
    }

    @RequestMapping("/fallback/ticket-service")
    public ResponseEntity<Map<String, Object>> ticketServiceFallback() {
        return createFallbackResponse("Ticket service is temporarily unavailable");
    }

    @RequestMapping("/fallback/inventory-service")
    public ResponseEntity<Map<String, Object>> inventoryServiceFallback() {
        return createFallbackResponse("Inventory service is temporarily unavailable");
    }

    @RequestMapping("/fallback/payment-service")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        return createFallbackResponse("Payment service is temporarily unavailable");
    }

    @RequestMapping("/fallback/notification-service")
    public ResponseEntity<Map<String, Object>> notificationServiceFallback() {
        return createFallbackResponse("Notification service is temporarily unavailable");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("statusCode", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("errorCode", "SERVICE_UNAVAILABLE");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
```

**Khi nào Fallback được gọi?**
- Khi Circuit Breaker **OPEN** (service lỗi quá nhiều)
- Thay vì chờ timeout, trả về response ngay lập tức

---

## 7. JWT VÀ KEYCLOAK TOKEN VALIDATOR

### 7.1. JWT Là Gì?

**JWT (JSON Web Token)** gồm 3 phần, phân cách bằng dấu `.`:

```
eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIn0.signature
     │                       │                    │
  HEADER                  PAYLOAD              SIGNATURE
```

**Ví dụ Payload sau khi decode:**
```json
{
  "sub": "user123",
  "preferred_username": "john",
  "realm_access": {
    "roles": ["USER", "ADMIN"]
  },
  "exp": 1737100800,
  "iss": "http://localhost:8080/realms/train-ticket"
}
```

### 7.2. KeycloakTokenValidator

```57:117:apigateway/src/main/java/com/example/apigateway/util/KeycloakTokenValidator.java
    public boolean validateToken(String token) {
        try {
            // Parse token without verification first to get header
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                logger.debug("Invalid token format");
                return false;
            }

            // Decode header to get key ID (kid)
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            String kid = header.get("kid").asText();

            // Get public key from Keycloak
            PublicKey publicKey = getPublicKey(kid).block();
            if (publicKey == null) {
                logger.warn("Could not retrieve public key from Keycloak");
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
                logger.warn("Token issuer mismatch. Expected: {}, Got: {}", expectedIssuer, issuer);
                return false;
            }

            // Validate audience (client ID)
            String audience = claims.getAudience().stream().findFirst().orElse("");
            if (!audience.equals(keycloakConfig.getClientId())) {
                logger.warn("Token audience mismatch. Expected: {}, Got: {}", keycloakConfig.getClientId(), audience);
                return false;
            }

            // Check expiration
            if (claims.getExpiration().before(new java.util.Date())) {
                logger.debug("Token has expired");
                return false;
            }

            return true;

        } catch (ExpiredJwtException e) {
            logger.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (SecurityException e) {
            logger.debug("Token signature validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error validating Keycloak token", e);
            return false;
        }
    }
```

**Luồng validate:**
1. Tách token thành 3 phần
2. Decode header để lấy `kid` (Key ID)
3. Gọi Keycloak JWKS endpoint để lấy public key
4. Verify signature bằng public key
5. Kiểm tra `issuer`, `audience`, `expiration`

---

## 8. TÓM TẮT KIẾN TRÚC

```
                         ┌──────────────────────────────────┐
                         │          API GATEWAY             │
                         │         (Port 8080)              │
                         ├──────────────────────────────────┤
  Request ──────────────►│  1. LoggingFilter               │
                         │     - Tạo Correlation ID         │
                         │     - Log request/response       │
                         ├──────────────────────────────────┤
                         │  2. RateLimitFilter              │
                         │     - Giới hạn 100 req/min       │
                         │     - 10 req/min cho auth        │
                         ├──────────────────────────────────┤
                         │  3. JwtAuthenticationFilter      │
                         │     - Validate Keycloak token    │
                         │     - Check roles                │
                         │     - Add user headers           │
                         ├──────────────────────────────────┤
                         │  4. Routing                      │
                         │     - /api/users/** → user-svc   │
                         │     - /api/tickets/** → ticket   │
                         │     - Circuit Breaker            │
                         ├──────────────────────────────────┤
  Error ◄────────────────│  5. GlobalErrorFilter            │
                         │     - Xử lý exception            │
                         │     - Response JSON nhất quán    │
                         └──────────────────────────────────┘
                                        │
               ┌────────────┬───────────┼───────────┬────────────┐
               ▼            ▼           ▼           ▼            ▼
          ┌────────┐  ┌────────┐  ┌──────────┐  ┌────────┐  ┌──────────┐
          │ User   │  │ Ticket │  │Inventory │  │Payment │  │Notifica- │
          │Service │  │Service │  │ Service  │  │Service │  │  tion    │
          └────────┘  └────────┘  └──────────┘  └────────┘  └──────────┘
```

---

## 9. NHỮNG ĐIỂM QUAN TRỌNG CẦN NHỚ

| Khái niệm | Mục đích |
|-----------|----------|
| **API Gateway** | Điểm vào duy nhất, routing, authentication |
| **Service Discovery (Eureka)** | Tự động tìm kiếm service, không cần hardcode IP |
| **Circuit Breaker** | Ngăn cascading failure, trả fallback khi service lỗi |
| **Rate Limiting** | Bảo vệ khỏi quá tải và DDoS |
| **JWT/Keycloak** | Xác thực và phân quyền tập trung |
| **Correlation ID** | Theo dõi request xuyên suốt các service |
| **Reactive (WebFlux)** | Xử lý nhiều request đồng thời, non-blocking |

Nếu bạn có câu hỏi cụ thể về phần nào, hãy hỏi thêm nhé!