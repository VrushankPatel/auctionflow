package com.auctionflow.core.domain.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class FailedLoginEvent extends SecurityEvent {
    private final String username;
    private final String reason;

    public FailedLoginEvent(UUID eventId, Instant timestamp, String ipAddress, String userAgent, String username, String reason, Map<String, Object> details) {
        super(eventId, timestamp, "FAILED_LOGIN", ipAddress, userAgent, details);
        this.username = username;
        this.reason = reason;
    }

    public String getUsername() {
        return username;
    }

    public String getReason() {
        return reason;
    }
}