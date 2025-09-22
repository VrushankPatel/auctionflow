package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.WinnerId;

import java.time.Instant;
import java.util.UUID;

public class AuctionClosedEvent extends DomainEvent {
    private final WinnerId winnerId;

    public AuctionClosedEvent(AuctionId auctionId, WinnerId winnerId, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(auctionId, eventId, timestamp, sequenceNumber);
        this.winnerId = winnerId;
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        // winnerId can be null if no bids
    }

    public WinnerId getWinnerId() {
        return winnerId;
    }
}