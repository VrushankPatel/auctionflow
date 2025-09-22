package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.time.Instant;
import java.util.UUID;

public abstract class DomainEvent {
    protected final AuctionId aggregateId;
    protected final UUID eventId;
    protected final Instant timestamp;
    protected final long sequenceNumber;

    protected DomainEvent(AuctionId aggregateId, UUID eventId, Instant timestamp, long sequenceNumber) {
        this.aggregateId = aggregateId;
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
    }

    public AuctionId getAggregateId() {
        return aggregateId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }
}