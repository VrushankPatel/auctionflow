package com.auctionflow.core.domain.events;

import java.time.Instant;
import java.util.UUID;

public class DisputeCreatedEvent extends DomainEvent {
    private final String auctionId;
    private final String initiatorId;
    private final String reason;
    private final String description;

    public DisputeCreatedEvent(String auctionId, String initiatorId, String reason, String description, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(null, eventId, timestamp, sequenceNumber); // No aggregateId for disputes?
        this.auctionId = auctionId;
        this.initiatorId = initiatorId;
        this.reason = reason;
        this.description = description;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public String getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }
}