# Train Ticket Booking System

Hệ thống đặt vé tàu high-performance với kiến trúc microservices, được thiết kế để xử lý nhiều request đồng thời, chịu tải cao và dễ dàng scale.

## Kiến trúc

Hệ thống sử dụng kiến trúc microservices với các service sau:

- **API Gateway**: Entry point cho tất cả requests, xử lý routing, authentication, rate limiting
- **User Service**: Quản lý người dùng, authentication và authorization với JWT
- **Ticket Service**: Quản lý đặt vé, hủy vé, tra cứu vé
- **Inventory Service**: Quản lý số lượng ghế, real-time availability với distributed locks
- **Payment Service**: Xử lý thanh toán
- **Notification Service**: Gửi thông báo email/SMS qua message queue
- **Eureka Server**: Service discovery

## Công nghệ sử dụng

- **Spring Boot 3.5.0** với Java 17
- **Spring Cloud Gateway** cho API Gateway
- **Spring Cloud Eureka** cho service discovery
- **PostgreSQL** cho database chính
- **Redis** cho caching và distributed locks
- **RabbitMQ** cho message queue
- **JWT** cho authentication
- **Resilience4j** cho circuit breaker và rate limiting
- **HikariCP** cho connection pooling

## Yêu cầu

- Java 17+
- Docker và Docker Compose
- Gradle (hoặc sử dụng Gradle Wrapper)

## Task Runner - Poe

Project sử dụng **Poe the Poet** để quản lý commands dễ dàng.

### Sử dụng Poe

Xem tất cả tasks:
```bash
poe
```

Các commands phổ biến:
```bash
poe up            # Build và chạy tất cả services
poe up-d          # Chạy ở background
poe down          # Dừng tất cả services
poe logs          # Xem logs
poe logs-service apigateway  # Logs của một service
poe health        # Kiểm tra health của tất cả services
poe run-user      # Chạy User Service locally
poe build-all     # Build tất cả services
poe db-connect    # Kết nối PostgreSQL
poe redis-cli      # Kết nối Redis
```

### Cài đặt Poe (nếu chưa có)

```bash
pip install poethepoet
# hoặc
pipx install poethepoet
```

Xem file `.poe-help.md` để biết danh sách đầy đủ các tasks.

## Cài đặt và chạy

### Cách 1: Sử dụng Task Runner (Khuyến nghị)

**Với Justfile:**
```bash
just up-d          # Khởi động tất cả services
just logs          # Xem logs
just health        # Kiểm tra health
```

**Với Makefile:**
```bash
make up-d          # Khởi động tất cả services
make logs          # Xem logs
make health        # Kiểm tra health
```

### Cách 2: Sử dụng Docker Compose trực tiếp

**1. Khởi động infrastructure services:**

```bash
docker-compose up -d
```

Lệnh này sẽ khởi động:
- PostgreSQL (port 5432)
- Redis (port 6379)
- RabbitMQ (port 5672, Management UI: http://localhost:15672)

### 2. Khởi động Eureka Server

```bash
cd eureka-server
./gradlew bootRun
```

Eureka Server sẽ chạy tại: http://localhost:8761

### 3. Khởi động các microservices

Mở các terminal riêng và chạy từng service:

**User Service:**
```bash
cd user-service
./gradlew bootRun
```

**Inventory Service:**
```bash
cd inventory-service
./gradlew bootRun
```

**Ticket Service:**
```bash
cd ticket-service
./gradlew bootRun
```

**Payment Service:**
```bash
cd payment-service
./gradlew bootRun
```

**Notification Service:**
```bash
cd notification-service
./gradlew bootRun
```

**API Gateway:**
```bash
cd apigateway
./gradlew bootRun
```

## API Endpoints

Tất cả requests đều đi qua API Gateway tại `http://localhost:8080`

### Authentication (không cần JWT)

- `POST /api/users/register` - Đăng ký tài khoản
- `POST /api/users/login` - Đăng nhập

### Ticket Booking (cần JWT token)

- `POST /api/tickets/book` - Đặt vé
- `GET /api/tickets/{id}` - Lấy thông tin vé
- `GET /api/tickets/user/{userId}` - Lấy danh sách vé của user
- `POST /api/tickets/{id}/confirm` - Xác nhận vé
- `POST /api/tickets/{id}/cancel` - Hủy vé

### Inventory (cần JWT token)

- `GET /api/inventory/availability?trainId={id}&departureDate={date}` - Kiểm tra số ghế còn lại
- `POST /api/inventory/reserve` - Giữ ghế
- `POST /api/inventory/release` - Trả ghế

### Payment (cần JWT token)

- `POST /api/payments/process` - Xử lý thanh toán
- `POST /api/payments/{id}/refund` - Hoàn tiền
- `GET /api/payments/user/{userId}` - Lấy danh sách thanh toán của user

## Tính năng Performance

### Caching
- Redis cache cho tickets và inventory data
- Cache invalidation khi có thay đổi
- TTL cho cache entries

### Concurrency Control
- Optimistic locking với @Version trong entities
- Distributed locks với Redisson cho seat reservation
- Database transactions cho critical operations

### Rate Limiting
- API Gateway level rate limiting
- Redis-based sliding window counter

### Connection Pooling
- HikariCP với cấu hình tối ưu
- Connection pool size: 20
- Idle timeout và max lifetime được cấu hình

### Async Processing
- RabbitMQ cho notifications
- Non-blocking operations cho heavy tasks

## Monitoring

Tất cả services đều có Spring Boot Actuator enabled:
- Health checks: `http://localhost:{port}/actuator/health`
- Metrics: `http://localhost:{port}/actuator/metrics`

## Database Schema

Các bảng chính:
- `users`: Thông tin người dùng
- `trains`: Thông tin chuyến tàu
- `seats`: Thông tin ghế
- `tickets`: Thông tin vé
- `bookings`: Thông tin đặt chỗ
- `payments`: Thông tin thanh toán
- `inventory`: Số lượng ghế còn lại theo chuyến
- `notifications`: Lịch sử thông báo

## Scalability

- Mỗi service có thể scale độc lập
- Stateless services, dễ scale ngang
- Redis cluster mode cho cache scaling
- RabbitMQ cluster cho message queue scaling
- Database read replicas cho read-heavy operations

## Security

- JWT-based authentication
- API Gateway làm authentication point
- Input validation và sanitization
- SQL injection prevention (JPA/Hibernate)
- Rate limiting để prevent DDoS

## Development Notes

- Tất cả services sử dụng auto-ddl (update mode) cho development
- JWT secret được hardcode trong config (nên sử dụng environment variables trong production)
- Payment gateway được simulate (cần tích hợp thực tế)
- Email/SMS notifications được simulate (cần tích hợp thực tế)

