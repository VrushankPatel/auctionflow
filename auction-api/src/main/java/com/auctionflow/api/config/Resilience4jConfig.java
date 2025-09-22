package com.auctionflow.api.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.redis.RedisRateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry(RedisConnectionFactory redisConnectionFactory) {
        RateLimiterRegistry registry = RedisRateLimiterRegistry.of(redisConnectionFactory).build();

        // Register rate limiters
        registry.rateLimiter("perUser", perUserRateLimiterConfig());
        registry.rateLimiter("perIp", perIpRateLimiterConfig());
        registry.rateLimiter("perAuction", perAuctionRateLimiterConfig());

        return registry;
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

        // Register circuit breakers
        registry.circuitBreaker("redis", redisCircuitBreakerConfig());
        registry.circuitBreaker("kafka", kafkaCircuitBreakerConfig());

        return registry;
    }

    // Define rate limiter configs
    private RateLimiterConfig perUserRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(5) // 5 bids per second
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build();
    }

    private RateLimiterConfig perIpRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(10) // example: 10 requests per second per IP
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build();
    }

    private RateLimiterConfig perAuctionRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(100) // 100 bids per second per auction
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build();
    }

    // Circuit breaker for external services
    private CircuitBreakerConfig redisCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(10)
                .build();
    }

    private CircuitBreakerConfig kafkaCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(2000))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(10)
                .build();
    }
}