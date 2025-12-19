package com.example.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	@Bean
	public TopicExchange bookingExchange() {
		return new TopicExchange("booking.exchange");
	}

	@Bean
	public TopicExchange paymentExchange() {
		return new TopicExchange("payment.exchange");
	}

	@Bean
	public Queue bookingNotificationQueue() {
		return QueueBuilder.durable("booking.notification.queue").build();
	}

	@Bean
	public Queue paymentNotificationQueue() {
		return QueueBuilder.durable("payment.notification.queue").build();
	}

	@Bean
	public Binding bookingNotificationBinding() {
		return BindingBuilder
				.bind(bookingNotificationQueue())
				.to(bookingExchange())
				.with("notification.booking");
	}

	@Bean
	public Binding paymentNotificationBinding() {
		return BindingBuilder
				.bind(paymentNotificationQueue())
				.to(paymentExchange())
				.with("notification.payment");
	}
}

