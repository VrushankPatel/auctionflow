package com.auctionflow.core.domain.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class SuspiciousActivityEvent extends SecurityEvent {
    private final String activityType;
    private final String description;

    public SuspiciousActivityEvent(UUID eventId, Instant timestamp, String ipAddress, String userAgent, String activityType, String description, Map<String, Object> details) {
        super(eventId, timestamp, "SUSPICIOUS_ACTIVITY", ipAddress, userAgent, details);
        this.activityType = activityType;
        this.description = description;
    }

    public String getActivityType() {
        return activityType;
    }

    public String getDescription() {
        return description;
    }
}