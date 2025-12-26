package com.example.shared.constant;

public final class AppConstants {
    
    private AppConstants() {
        // Utility class
    }
    
    // Header constants
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    
    // Cache names
    public static final String CACHE_USERS = "users";
    public static final String CACHE_TRAINS = "trains";
    public static final String CACHE_ROUTES = "routes";
    public static final String CACHE_SCHEDULES = "schedules";
    public static final String CACHE_INVENTORY = "inventory";
    public static final String CACHE_TICKETS = "tickets";
    public static final String CACHE_USER_TICKETS = "userTickets";
    
    // Kafka topics
    public static final String TOPIC_BOOKING_EVENTS = "booking-events";
    public static final String TOPIC_PAYMENT_EVENTS = "payment-events";
    public static final String TOPIC_NOTIFICATION_EVENTS = "notification-events";
    public static final String TOPIC_INVENTORY_EVENTS = "inventory-events";
    
    // RabbitMQ exchanges and queues
    public static final String EXCHANGE_BOOKING = "booking.exchange";
    public static final String EXCHANGE_PAYMENT = "payment.exchange";
    public static final String EXCHANGE_NOTIFICATION = "notification.exchange";
    
    public static final String QUEUE_BOOKING_CREATED = "booking.created.queue";
    public static final String QUEUE_BOOKING_CANCELLED = "booking.cancelled.queue";
    public static final String QUEUE_PAYMENT_COMPLETED = "payment.completed.queue";
    public static final String QUEUE_NOTIFICATION_EMAIL = "notification.email.queue";
    public static final String QUEUE_NOTIFICATION_SMS = "notification.sms.queue";
    
    // Pagination defaults
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";
    
    // Redis key prefixes
    public static final String REDIS_USER_SESSION = "user:session:";
    public static final String REDIS_USER_CACHE = "user:cache:";
    public static final String REDIS_INVENTORY_LOCK = "inventory:lock:";
    public static final String REDIS_RATE_LIMIT = "rate:limit:";
    
    // Ticket status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    
    // Payment status
    public static final String PAYMENT_PENDING = "PENDING";
    public static final String PAYMENT_COMPLETED = "COMPLETED";
    public static final String PAYMENT_FAILED = "FAILED";
    public static final String PAYMENT_REFUNDED = "REFUNDED";
    
    // User roles
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_OPERATOR = "ROLE_OPERATOR";
    
    // Seat classes
    public static final String SEAT_CLASS_ECONOMY = "ECONOMY";
    public static final String SEAT_CLASS_BUSINESS = "BUSINESS";
    public static final String SEAT_CLASS_FIRST = "FIRST";
    
    // Time constants
    public static final int BOOKING_TIMEOUT_MINUTES = 15;
    public static final int SESSION_TIMEOUT_HOURS = 24;
    public static final int TOKEN_EXPIRY_HOURS = 24;
    public static final int REFRESH_TOKEN_EXPIRY_DAYS = 7;
}

