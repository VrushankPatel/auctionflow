package com.auctionflow.core.domain.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public abstract class SecurityEvent {
    protected final UUID eventId;
    protected final Instant timestamp;
    protected final String eventType;
    protected final String ipAddress;
    protected final String userAgent;
    protected final Map<String, Object> details;

    protected SecurityEvent(UUID eventId, Instant timestamp, String eventType, String ipAddress, String userAgent, Map<String, Object> details) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.details = details;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}