package com.auctionflow.core.domain.events;

import java.time.Instant;
import java.util.UUID;

public abstract class DomainEvent {
    protected final Object aggregateId;
    protected final UUID eventId;
    protected final Instant timestamp;
    protected final long sequenceNumber;

    protected DomainEvent(Object aggregateId, UUID eventId, Instant timestamp, long sequenceNumber) {
        this.aggregateId = aggregateId;
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
    }

    public Object getAggregateId() {
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