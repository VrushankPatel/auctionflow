package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;
import java.util.UUID;

public class BidRejectedEvent extends DomainEvent {
    private final UUID bidderId;
    private final Money amount;
    private final String reason;

    public BidRejectedEvent(AuctionId auctionId, UUID bidderId, Money amount, String reason, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(auctionId, eventId, timestamp, sequenceNumber);
        this.bidderId = bidderId;
        this.amount = amount;
        this.reason = reason;
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        if (bidderId == null) {
            throw new IllegalArgumentException("BidderId cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be null or blank");
        }
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public Money getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }
}