package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.time.Instant;
import java.util.UUID;

public class AuctionRevealPhaseStartedEvent extends DomainEvent {
    private final AuctionId auctionId;
    private final Instant revealEndTime;

    public AuctionRevealPhaseStartedEvent(AuctionId auctionId, Instant revealEndTime,
                                          UUID eventId, Instant timestamp, long sequenceNumber) {
        super(eventId, timestamp, sequenceNumber);
        this.auctionId = auctionId;
        this.revealEndTime = revealEndTime;
    }

    public AuctionId getAuctionId() {
        return auctionId;
    }

    public Instant getRevealEndTime() {
        return revealEndTime;
    }

    @Override
    public AuctionId getAggregateId() {
        return auctionId;
    }
}