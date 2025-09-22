package com.auctionflow.api;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rate-limits")
public class RateLimitController {

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRateLimits() {
        Map<String, Object> limits = new HashMap<>();

        // Get bid rate limiter metrics
        RateLimiter bidLimiter = rateLimiterRegistry.rateLimiter("bidLimiter");
        if (bidLimiter != null) {
            limits.put("bidLimit", 5); // hardcoded for now
            limits.put("bidUsage", bidLimiter.getMetrics().getNumberOfWaitingThreads());
        }

        // General API limits (placeholder)
        limits.put("apiLimit", 100);
        limits.put("apiUsage", 0); // Would need to implement actual tracking

        return ResponseEntity.ok(limits);
    }
}