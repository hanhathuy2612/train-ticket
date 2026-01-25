# OpenAPI/Swagger Setup - Aggregate All Services

## 📋 Tổng quan

Gateway đã được cấu hình để aggregate OpenAPI/Swagger documentation từ tất cả microservices, cho phép truy cập tất cả APIs từ một Swagger UI duy nhất.

## ✅ Cấu hình đã thực hiện

### 1. Routes để Proxy OpenAPI Endpoints

**File:** `apigateway/src/main/resources/application.yml`

Đã thêm routes để proxy OpenAPI endpoints từ các microservices:

```yaml
# OpenAPI/Swagger routes - Proxy OpenAPI docs from microservices
- id: user-service-openapi
  uri: lb://user-service
  predicates:
    - Path=/api/users/v3/api-docs/**
  filters:
    - RewritePath=/api/users/(?<segment>.*), /$\{segment}

# Tương tự cho các services khác...
```

**Các routes:**
- `/api/users/v3/api-docs/**` → `user-service/v3/api-docs/**`
- `/api/tickets/v3/api-docs/**` → `ticket-service/v3/api-docs/**`
- `/api/inventory/v3/api-docs/**` → `inventory-service/v3/api-docs/**`
- `/api/payments/v3/api-docs/**` → `payment-service/v3/api-docs/**`
- `/api/notifications/v3/api-docs/**` → `notification-service/v3/api-docs/**`

### 2. Swagger UI URLs Configuration

**File:** `apigateway/src/main/resources/application.yml`

Đã cấu hình Swagger UI để hiển thị multiple service APIs:

```yaml
springdoc:
  swagger-ui:
    urls:
      - name: All Services (Gateway)
        url: /v3/api-docs
        display-name: All Services
      - name: User Service
        url: /api/users/v3/api-docs
        display-name: User Service API
      - name: Ticket Service
        url: /api/tickets/v3/api-docs
        display-name: Ticket Service API
      - name: Inventory Service
        url: /api/inventory/v3/api-docs
        display-name: Inventory Service API
      - name: Payment Service
        url: /api/payments/v3/api-docs
        display-name: Payment Service API
      - name: Notification Service
        url: /api/notifications/v3/api-docs
        display-name: Notification Service API
```

### 3. Public Endpoints

**File:** `apigateway/src/main/java/com/example/apigateway/filter/JwtAuthenticationFilter.java`

Đã thêm OpenAPI endpoints vào public paths:

```java
// OpenAPI/Swagger endpoints
"/swagger-ui.html",
"/swagger-ui/**",
"/v3/api-docs",
"/v3/api-docs/**",
// OpenAPI endpoints from microservices
"/api/users/v3/api-docs",
"/api/tickets/v3/api-docs",
"/api/inventory/v3/api-docs",
"/api/payments/v3/api-docs",
"/api/notifications/v3/api-docs",
```

## 🚀 Cách sử dụng

### Truy cập Swagger UI

**URL:** `http://localhost:8189/swagger-ui.html`

### Chọn Service trong Swagger UI

1. Mở Swagger UI: `http://localhost:8189/swagger-ui.html`
2. Ở góc trên bên phải, có dropdown **"Select a definition"**
3. Chọn service muốn xem:
   - **All Services (Gateway)** - Xem tất cả APIs (nếu có)
   - **User Service API** - Chỉ APIs từ User Service
   - **Ticket Service API** - Chỉ APIs từ Ticket Service
   - **Inventory Service API** - Chỉ APIs từ Inventory Service
   - **Payment Service API** - Chỉ APIs từ Payment Service
   - **Notification Service API** - Chỉ APIs từ Notification Service

### Truy cập OpenAPI JSON trực tiếp

- **Gateway:** `http://localhost:8189/v3/api-docs`
- **User Service:** `http://localhost:8189/api/users/v3/api-docs`
- **Ticket Service:** `http://localhost:8189/api/tickets/v3/api-docs`
- **Inventory Service:** `http://localhost:8189/api/inventory/v3/api-docs`
- **Payment Service:** `http://localhost:8189/api/payments/v3/api-docs`
- **Notification Service:** `http://localhost:8189/api/notifications/v3/api-docs`

## 🔍 Kiểm tra

### 1. Kiểm tra Routes hoạt động

```bash
# Test User Service OpenAPI
curl http://localhost:8189/api/users/v3/api-docs

# Test Ticket Service OpenAPI
curl http://localhost:8189/api/tickets/v3/api-docs
```

**Expected:** JSON response với OpenAPI spec từ service tương ứng

### 2. Kiểm tra Swagger UI

1. Mở browser: `http://localhost:8189/swagger-ui.html`
2. Kiểm tra dropdown "Select a definition" có các services
3. Chọn từng service và verify APIs hiển thị đúng

## ⚙️ Cấu hình chi tiết

### Route Pattern

Routes sử dụng `RewritePath` filter để:
- Giữ nguyên path structure (`/v3/api-docs/**`)
- Proxy đến microservice đúng cách
- Không strip prefix để giữ nguyên OpenAPI paths

### Authentication

Tất cả OpenAPI endpoints đều là **public** (không cần JWT token) để:
- Dễ dàng truy cập documentation
- Test APIs từ Swagger UI
- Debug và development

### Service Discovery

Routes sử dụng `lb://` (load balancer) với Eureka:
- Tự động discover service instances
- Load balance giữa các instances
- Fallback nếu service không available

## 🐛 Troubleshooting

### 1. Swagger UI không hiển thị services

**Nguyên nhân:**
- Routes chưa được cấu hình đúng
- Services chưa đăng ký với Eureka
- OpenAPI endpoints không accessible

**Giải pháp:**
- Kiểm tra routes trong `application.yml`
- Verify services đã register với Eureka: `http://localhost:8761`
- Test OpenAPI endpoints trực tiếp: `curl http://localhost:8189/api/users/v3/api-docs`

### 2. OpenAPI endpoints trả về 404

**Nguyên nhân:**
- Service không có OpenAPI/Swagger configured
- Route path không match
- RewritePath filter không đúng

**Giải pháp:**
- Kiểm tra service có `springdoc` dependency và config
- Verify route predicates match đúng path
- Check RewritePath pattern

### 3. Swagger UI không load được API docs

**Nguyên nhân:**
- CORS issues
- Service không accessible
- URL không đúng

**Giải pháp:**
- Kiểm tra CORS config trong `SecurityConfig`
- Verify service health: `http://localhost:8189/actuator/health`
- Check browser console for errors

## 📝 Notes

1. **Service URLs:** Tất cả URLs trong Swagger UI đều qua Gateway (`http://localhost:8189`)
2. **Authentication:** Khi test APIs từ Swagger UI, cần add JWT token vào "Authorize" button
3. **Service Discovery:** Đảm bảo Eureka Server đang chạy và services đã register
4. **Path Rewriting:** Routes sử dụng `RewritePath` để giữ nguyên OpenAPI path structure

## ✅ Checklist

- [x] Routes để proxy OpenAPI endpoints từ các services
- [x] Swagger UI URLs configuration
- [x] Public endpoints cho OpenAPI
- [x] RewritePath filters đúng
- [ ] Test với real services
- [ ] Verify tất cả services hiển thị trong Swagger UI
