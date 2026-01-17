package com.example.inventoryservice.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RedissonConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedissonConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        logger.info("Creating RedissonClient with address: redis://{}:{}", redisHost, redisPort);
        Config config = new Config();
        String address = String.format("redis://%s:%d", redisHost, redisPort);

        SingleServerConfig singleServerConfig = config.useSingleServer();
        singleServerConfig.setAddress(address);
        singleServerConfig.setConnectionMinimumIdleSize(10);
        singleServerConfig.setConnectionPoolSize(50);
        singleServerConfig.setIdleConnectionTimeout(10000);
        singleServerConfig.setConnectTimeout(10000);
        singleServerConfig.setTimeout(3000);
        singleServerConfig.setRetryAttempts(3);
        singleServerConfig.setRetryInterval(1500);

        config.setThreads(16);
        config.setNettyThreads(32);

        return Redisson.create(config);
    }
}
