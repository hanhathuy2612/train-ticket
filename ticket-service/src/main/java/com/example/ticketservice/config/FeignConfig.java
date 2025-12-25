package com.example.ticketservice.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.Retryer;
import feign.codec.ErrorDecoder;

@Configuration
@EnableFeignClients(basePackages = "com.example.ticketservice.client")
public class FeignConfig {

	/**
	 * Feign logger level for debugging
	 */
	@Bean
	public Logger.Level feignLoggerLevel() {
		return Logger.Level.BASIC;
	}

	/**
	 * Retry configuration for Feign clients
	 * Retry up to 3 times with 1 second initial interval
	 */
	@Bean
	public Retryer feignRetryer() {
		return new Retryer.Default(
				1000,   // Initial interval
				2000,   // Max interval
				3       // Max attempts
		);
	}

	/**
	 * Custom error decoder for Feign errors
	 */
	@Bean
	public ErrorDecoder errorDecoder() {
		return new CustomFeignErrorDecoder();
	}

	/**
	 * Custom error decoder implementation
	 */
	public static class CustomFeignErrorDecoder implements ErrorDecoder {
		
		private final ErrorDecoder defaultDecoder = new Default();

		@Override
		public Exception decode(String methodKey, feign.Response response) {
			// Log the error
			org.slf4j.LoggerFactory.getLogger(CustomFeignErrorDecoder.class)
					.error("Feign error: {} - status: {}", methodKey, response.status());

			// Use default decoder for now
			return defaultDecoder.decode(methodKey, response);
		}
	}
}
