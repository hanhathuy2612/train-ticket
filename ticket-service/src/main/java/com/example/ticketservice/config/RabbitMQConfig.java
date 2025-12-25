package com.example.ticketservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	@Value("${rabbitmq.exchange.booking:booking-exchange}")
	private String bookingExchange;

	@Value("${rabbitmq.queue.booking-created:booking-created-queue}")
	private String bookingCreatedQueue;

	@Value("${rabbitmq.queue.booking-confirmed:booking-confirmed-queue}")
	private String bookingConfirmedQueue;

	@Value("${rabbitmq.queue.booking-cancelled:booking-cancelled-queue}")
	private String bookingCancelledQueue;

	@Value("${rabbitmq.routing-key.booking-created:booking.created}")
	private String bookingCreatedRoutingKey;

	@Value("${rabbitmq.routing-key.booking-confirmed:booking.confirmed}")
	private String bookingConfirmedRoutingKey;

	@Value("${rabbitmq.routing-key.booking-cancelled:booking.cancelled}")
	private String bookingCancelledRoutingKey;

	// Exchange
	@Bean
	public DirectExchange bookingExchange() {
		return new DirectExchange(bookingExchange, true, false);
	}

	// Queues with DLQ support
	@Bean
	public Queue bookingCreatedQueue() {
		return QueueBuilder.durable(bookingCreatedQueue)
				.withArgument("x-dead-letter-exchange", bookingExchange + ".dlx")
				.withArgument("x-dead-letter-routing-key", bookingCreatedRoutingKey + ".dlq")
				.build();
	}

	@Bean
	public Queue bookingConfirmedQueue() {
		return QueueBuilder.durable(bookingConfirmedQueue)
				.withArgument("x-dead-letter-exchange", bookingExchange + ".dlx")
				.withArgument("x-dead-letter-routing-key", bookingConfirmedRoutingKey + ".dlq")
				.build();
	}

	@Bean
	public Queue bookingCancelledQueue() {
		return QueueBuilder.durable(bookingCancelledQueue)
				.withArgument("x-dead-letter-exchange", bookingExchange + ".dlx")
				.withArgument("x-dead-letter-routing-key", bookingCancelledRoutingKey + ".dlq")
				.build();
	}

	// Bindings
	@Bean
	public Binding bookingCreatedBinding() {
		return BindingBuilder
				.bind(bookingCreatedQueue())
				.to(bookingExchange())
				.with(bookingCreatedRoutingKey);
	}

	@Bean
	public Binding bookingConfirmedBinding() {
		return BindingBuilder
				.bind(bookingConfirmedQueue())
				.to(bookingExchange())
				.with(bookingConfirmedRoutingKey);
	}

	@Bean
	public Binding bookingCancelledBinding() {
		return BindingBuilder
				.bind(bookingCancelledQueue())
				.to(bookingExchange())
				.with(bookingCancelledRoutingKey);
	}

	// Message Converter
	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	// RabbitTemplate with JSON converter
	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(jsonMessageConverter());
		return rabbitTemplate;
	}
}

