# CORS Best Practices cho Spring Cloud Gateway + Keycloak JWT

## 📋 Tổng quan

Tài liệu này mô tả cấu hình CORS đúng cách cho Spring Cloud Gateway với Keycloak JWT authentication, dựa trên best practices 2024.

## ✅ Cấu hình hiện tại

### 1. Application.yml - Global CORS

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        # 🔥 QUAN TRỌNG: Đảm bảo OPTIONS requests được xử lý
        add-to-simple-url-handler-mapping: true
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - http://localhost:4200
              - http://localhost:3000
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - PATCH
              - OPTIONS
            allowedHeaders:
              - "*"
            allowCredentials: true
            maxAge: 3600
```

**Lý do:**
- `add-to-simple-url-handler-mapping: true` - Đảm bảo OPTIONS preflight requests được xử lý bởi Gateway
- Không dùng `"*"` cho origins vì `allowCredentials: true` yêu cầu specific origins

### 2. SecurityConfig - CORS trong Spring Security

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200", "http://localhost:3000"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}

@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(ex -> ex
            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .anyExchange().permitAll())
        .build();
}
```

**Lý do:**
- `.cors()` - Bắt buộc để Spring Security xử lý CORS
- `OPTIONS permitAll()` - Bắt buộc để preflight requests không bị chặn
- Disable OAuth2 Resource Server vì dùng custom JwtAuthenticationFilter

### 3. JwtAuthenticationFilter - CORS cho error responses

```java
private void addCorsHeaders(HttpHeaders headers, ServerHttpRequest request) {
    String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);
    if (origin != null && !origin.isEmpty()) {
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    } else {
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    }
    headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, PATCH, OPTIONS");
    headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "...");
    headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
}
```

**Lý do:**
- Error responses (401/403) cần có CORS headers để browser có thể đọc được

## 🔍 Flow xử lý CORS

```
Browser Request
  ↓
1. OPTIONS Preflight (nếu cần)
   → SecurityConfig (.cors() + OPTIONS permitAll) ✅
   → globalcors config (add-to-simple-url-handler-mapping: true) ✅
   → Response với CORS headers
  ↓
2. Actual Request (POST/GET/etc)
   → SecurityConfig (.cors()) ✅
   → JwtAuthenticationFilter (validate token)
   → Gateway routes
   → Backend services
   → Response với CORS headers (từ globalcors hoặc SecurityConfig)
```

## 🐛 Debug CORS Issues

### 1. Sử dụng Swagger UI

Truy cập: `http://localhost:8080/swagger-ui.html`

- Test API trực tiếp từ browser
- Xem request/response headers
- Kiểm tra CORS headers trong Network tab

### 2. Kiểm tra logs

```yaml
logging:
  level:
    org.springframework.web.cors: DEBUG
    org.springframework.security: DEBUG
    org.springframework.cloud.gateway: DEBUG
```

### 3. Common Issues

| Lỗi                              | Nguyên nhân                          | Giải pháp                     |
| -------------------------------- | ------------------------------------ | ----------------------------- |
| No 'Access-Control-Allow-Origin' | Thiếu `.cors()` trong SecurityConfig | Thêm `.cors()`                |
| OPTIONS 401                      | Security chặn OPTIONS                | Thêm `OPTIONS permitAll()`    |
| CORS ok nhưng 401                | Token validation fail                | Check JwtAuthenticationFilter |
| Postman ok, browser fail         | Postman không check CORS             | Test bằng browser/Swagger UI  |

## 📝 Checklist

- [x] `globalcors` config với `add-to-simple-url-handler-mapping: true`
- [x] `CorsConfigurationSource` bean trong SecurityConfig
- [x] `.cors()` trong SecurityWebFilterChain
- [x] `OPTIONS permitAll()` trong SecurityConfig
- [x] CORS headers trong error responses (JwtAuthenticationFilter, GlobalErrorFilter)
- [x] Disable OAuth2 Resource Server (vì dùng custom filter)
- [x] OpenAPI/Swagger UI để debug

## 🚀 Best Practices

1. **Không dùng `"*"` với `allowCredentials: true`**
   - Phải specify exact origins

2. **CORS phải được xử lý TRƯỚC authentication**
   - SecurityConfig xử lý CORS trước Gateway filters

3. **Error responses cũng cần CORS headers**
   - JwtAuthenticationFilter và GlobalErrorFilter đều thêm CORS headers

4. **Test bằng browser, không chỉ Postman**
   - Postman không enforce CORS policy

5. **Sử dụng Swagger UI để debug**
   - Xem request/response headers trực tiếp

## 📚 References

- [Spring Cloud Gateway CORS Documentation](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/cors-configuration.html)
- [Spring Security CORS Documentation](https://docs.spring.io/spring-security/reference/reactive/integrations/cors.html)
- [Keycloak JWT Best Practices](https://www.keycloak.org/docs/latest/securing_apps/)
