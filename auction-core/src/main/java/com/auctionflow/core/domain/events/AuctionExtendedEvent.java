package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.time.Instant;
import java.util.UUID;

public class AuctionExtendedEvent extends DomainEvent {
    private final Instant newEndTime;

    public AuctionExtendedEvent(AuctionId auctionId, Instant newEndTime, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(auctionId, eventId, timestamp, sequenceNumber);
        this.newEndTime = newEndTime;
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        if (newEndTime == null) {
            throw new IllegalArgumentException("NewEndTime cannot be null");
        }
    }

    public Instant getNewEndTime() {
        return newEndTime;
    }
}