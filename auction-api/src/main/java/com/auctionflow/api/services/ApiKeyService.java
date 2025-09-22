package com.auctionflow.api.services;

import com.auctionflow.api.entities.ApiKey;
import com.auctionflow.api.repositories.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class ApiKeyService {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String ALGORITHM = "SHA-256";
    private static final int KEY_LENGTH = 32; // 256 bits
    private static final int RATE_LIMIT_REQUESTS = 100; // per minute
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    public String generateKey(String serviceName) {
        String rawKey = generateRandomKey();
        String hashedKey = hashKey(rawKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setServiceName(serviceName);
        apiKey.setHashedKey(hashedKey);
        apiKey.setCreatedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        return rawKey; // Return the raw key to the caller
    }

    public void revokeKey(String serviceName) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByServiceNameAndRevokedAtIsNull(serviceName);
        if (apiKeyOpt.isPresent()) {
            ApiKey apiKey = apiKeyOpt.get();
            apiKey.setRevokedAt(Instant.now());
            apiKeyRepository.save(apiKey);
        }
    }

    public boolean validateKey(String rawKey) {
        String hashedKey = hashKey(rawKey);
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByHashedKeyAndRevokedAtIsNull(hashedKey);
        if (apiKeyOpt.isPresent()) {
            if (isRateLimitExceeded(hashedKey)) {
                return false;
            }
            trackUsage(apiKeyOpt.get());
            return true;
        }
        return false;
    }

    public void trackUsage(ApiKey apiKey) {
        apiKey.setLastUsedAt(Instant.now());
        apiKeyRepository.save(apiKey);
    }

    private boolean isRateLimitExceeded(String hashedKey) {
        String redisKey = "rate_limit:" + hashedKey;
        Long currentCount = redisTemplate.opsForValue().increment(redisKey);
        if (currentCount == 1) {
            // Set expiry on first increment
            redisTemplate.expire(redisKey, RATE_LIMIT_WINDOW);
        }
        return currentCount > RATE_LIMIT_REQUESTS;
    }

    // Rotate keys older than 90 days
    @Scheduled(fixedRate = 86400000) // Daily check
    public void rotateExpiredKeys() {
        // Find keys older than 90 days and revoke them
        // For simplicity, revoke all active keys older than 90 days
        // In practice, generate new ones and notify services
        // But for now, just revoke
        Instant ninetyDaysAgo = Instant.now().minus(Duration.ofDays(90));
        apiKeyRepository.findAll().stream()
            .filter(key -> key.getRevokedAt() == null && key.getCreatedAt().isBefore(ninetyDaysAgo))
            .forEach(key -> {
                key.setRevokedAt(Instant.now());
                apiKeyRepository.save(key);
            });
    }

    private String generateRandomKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[KEY_LENGTH];
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(key.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }
}