package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.aggregates.AggregateRoot;
import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for compensation events that undo the effects of previous events.
 * Compensation events should implement the logic to reverse the state changes.
 */
public abstract class CompensationEvent extends DomainEvent {

    protected final UUID originalEventId; // The event this compensates for

    protected CompensationEvent(AuctionId aggregateId, UUID eventId, Instant timestamp, long sequenceNumber, UUID originalEventId) {
        super(aggregateId, eventId, timestamp, sequenceNumber);
        this.originalEventId = originalEventId;
    }

    public UUID getOriginalEventId() {
        return originalEventId;
    }

    /**
     * Applies the compensation logic to the aggregate.
     * Subclasses should implement the inverse operation.
     */
    public abstract void compensate(AggregateRoot aggregate);
}