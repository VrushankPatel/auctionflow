package com.auctionflow.core.domain.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class RateLimitExceededEvent extends SecurityEvent {
    private final String endpoint;
    private final String limitType;

    public RateLimitExceededEvent(UUID eventId, Instant timestamp, String ipAddress, String userAgent, String endpoint, String limitType, Map<String, Object> details) {
        super(eventId, timestamp, "RATE_LIMIT_EXCEEDED", ipAddress, userAgent, details);
        this.endpoint = endpoint;
        this.limitType = limitType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getLimitType() {
        return limitType;
    }
}