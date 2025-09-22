package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;
import java.util.UUID;

public class BidPlacedEvent extends DomainEvent {
    private final UUID bidderId;
    private final Money amount;

    public BidPlacedEvent(AuctionId auctionId, UUID bidderId, Money amount, Instant timestamp, UUID eventId, long sequenceNumber) {
        super(auctionId, eventId, timestamp, sequenceNumber);
        this.bidderId = bidderId;
        this.amount = amount;
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        if (bidderId == null) {
            throw new IllegalArgumentException("BidderId cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public Money getAmount() {
        return amount;
    }
}