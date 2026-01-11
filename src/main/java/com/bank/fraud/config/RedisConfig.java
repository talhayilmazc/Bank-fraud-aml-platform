package com.bank.fraud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
public class RedisConfig {

    @Bean
    public JedisPool jedisPool(
            @Value("${app.redis.host}") String host,
            @Value("${app.redis.port}") int port
    ) {
        return new JedisPool(host, port);
    }
}
