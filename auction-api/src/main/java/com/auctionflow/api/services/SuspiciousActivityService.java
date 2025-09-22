package com.auctionflow.api.services;

import com.auctionflow.events.publisher.KafkaEventPublisher;
import com.auctionflow.core.domain.events.SuspiciousActivityEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SuspiciousActivityService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaEventPublisher eventPublisher;

    @Autowired
    public SuspiciousActivityService(RedisTemplate<String, String> redisTemplate, KafkaEventPublisher eventPublisher) {
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
    }

    public void checkForSuspiciousActivity(String userId, String ipAddress, String userAgent, String activityType) {
        // Example: Check for rapid bidding from same IP
        String key = "bids:" + ipAddress + ":" + userId;
        Long bidCount = redisTemplate.opsForValue().increment(key);
        if (bidCount == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS); // Reset every minute
        }

        if (bidCount > 10) { // More than 10 bids per minute from same IP/user
            publishSuspiciousActivity(ipAddress, userAgent, "RAPID_BIDDING", "User " + userId + " bidding rapidly from IP " + ipAddress, Map.of("bidCount", bidCount));
        }

        // Check for multiple failed logins
        String failedKey = "failed_logins:" + ipAddress;
        String failedCountStr = redisTemplate.opsForValue().get(failedKey);
        int failedCount = failedCountStr != null ? Integer.parseInt(failedCountStr) : 0;
        if (failedCount > 5) {
            publishSuspiciousActivity(ipAddress, userAgent, "MULTIPLE_FAILED_LOGINS", "Multiple failed logins from IP " + ipAddress, Map.of("failedCount", failedCount));
        }
    }

    public void recordFailedLogin(String ipAddress) {
        String key = "failed_logins:" + ipAddress;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 3600, TimeUnit.SECONDS); // Expire after 1 hour
    }

    public void recordSuspiciousActivity(Long userId, String activityType, String details) {
        publishSuspiciousActivity(
            "unknown", // IP address not available in this context
            "unknown", // User agent not available
            activityType,
            "Suspicious activity detected for user " + userId,
            Map.of("userId", userId.toString(), "details", details)
        );
    }

    private void publishSuspiciousActivity(String ipAddress, String userAgent, String activityType, String description, Map<String, Object> details) {
        SuspiciousActivityEvent event = new SuspiciousActivityEvent(
            java.util.UUID.randomUUID(),
            Instant.now(),
            ipAddress,
            userAgent,
            activityType,
            description,
            details
        );
        eventPublisher.publishSecurityEvent(event);
    }
}