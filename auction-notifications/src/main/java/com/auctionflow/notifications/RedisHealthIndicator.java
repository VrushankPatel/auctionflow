package com.auctionflow.notifications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public Health health() {
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            connection.ping();
            return Health.up().withDetail("redis", "Connected").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("redis", "Connection failed").build();
        }
    }
}