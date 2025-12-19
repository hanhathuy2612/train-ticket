# Hướng dẫn chạy hệ thống với Docker Compose

## Yêu cầu

- Docker và Docker Compose đã được cài đặt
- Ít nhất 4GB RAM trống
- Ports sau phải trống: 8080, 8081, 8082, 8083, 8084, 8085, 8761, 5432, 6379, 5672, 15672

## Cách chạy

### 1. Build và khởi động tất cả services

```bash
docker-compose up --build
```

Lệnh này sẽ:
- Build tất cả các Docker images cho các services
- Khởi động infrastructure (PostgreSQL, Redis, RabbitMQ)
- Khởi động Eureka Server
- Khởi động tất cả microservices
- Khởi động API Gateway

### 2. Chạy ở background (detached mode)

```bash
docker-compose up -d --build
```

### 3. Xem logs

Xem logs của tất cả services:
```bash
docker-compose logs -f
```

Xem logs của một service cụ thể:
```bash
docker-compose logs -f apigateway
docker-compose logs -f eureka-server
docker-compose logs -f user-service
```

### 4. Dừng services

```bash
docker-compose down
```

Dừng và xóa volumes (xóa dữ liệu):
```bash
docker-compose down -v
```

### 5. Restart một service cụ thể

```bash
docker-compose restart user-service
```

## Thứ tự khởi động

Docker Compose sẽ tự động khởi động theo thứ tự:

1. **Infrastructure** (PostgreSQL, Redis, RabbitMQ)
2. **Eureka Server** - Service Discovery
3. **Microservices** (User, Inventory, Payment, Notification)
4. **Ticket Service** (phụ thuộc vào Inventory Service)
5. **API Gateway** (phụ thuộc vào tất cả services)

## Kiểm tra services

### Eureka Dashboard
- URL: http://localhost:8761
- Xem danh sách tất cả services đã đăng ký

### API Gateway
- URL: http://localhost:8080
- Health: http://localhost:8080/actuator/health

### RabbitMQ Management
- URL: http://localhost:15672
- Username: admin
- Password: admin

### Health Checks

Tất cả services đều có health check endpoint:
- User Service: http://localhost:8081/actuator/health
- Inventory Service: http://localhost:8082/actuator/health
- Ticket Service: http://localhost:8083/actuator/health
- Payment Service: http://localhost:8084/actuator/health
- Notification Service: http://localhost:8085/actuator/health

## Troubleshooting

### Service không khởi động được

1. Kiểm tra logs:
```bash
docker-compose logs [service-name]
```

2. Kiểm tra health status:
```bash
docker-compose ps
```

3. Restart service:
```bash
docker-compose restart [service-name]
```

### Port đã được sử dụng

Nếu port đã được sử dụng, bạn có thể:
- Thay đổi port trong `docker-compose.yml`
- Hoặc dừng service đang sử dụng port đó

### Database connection error

Đảm bảo PostgreSQL đã khởi động hoàn toàn trước khi các services kết nối:
```bash
docker-compose up postgres
# Đợi vài giây
docker-compose up
```

### Build error

Nếu gặp lỗi build, thử:
```bash
docker-compose build --no-cache
docker-compose up
```

## Cấu trúc mạng

Tất cả services chạy trong cùng một Docker network (`train-ticket-network`) và có thể giao tiếp với nhau qua service names:
- `postgres` - PostgreSQL database
- `redis` - Redis cache
- `rabbitmq` - RabbitMQ message broker
- `eureka-server` - Eureka Server
- `user-service`, `ticket-service`, etc. - Microservices

## Environment Variables

Các services sử dụng environment variables để cấu hình:
- `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` - Địa chỉ Eureka Server
- `SPRING_DATASOURCE_URL` - Database connection string
- `SPRING_DATA_REDIS_HOST` - Redis host
- `SPRING_RABBITMQ_HOST` - RabbitMQ host

Các giá trị này được tự động set trong `docker-compose.yml` để sử dụng service names thay vì localhost.

