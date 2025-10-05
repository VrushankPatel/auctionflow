package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;
import java.util.UUID;

public class BidPlacedEvent extends DomainEvent {
    private final String bidderId;
    private final Money amount;
    private final long seqNo;

    public BidPlacedEvent(AuctionId auctionId, String bidderId, Money amount, Instant timestamp, UUID eventId, long sequenceNumber, long seqNo) {
        super(auctionId, eventId, timestamp, sequenceNumber);
        this.bidderId = bidderId;
        this.amount = amount;
        this.seqNo = seqNo;
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("BidderId cannot be null or blank");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public String getBidderId() {
        return bidderId;
    }

    public Money getAmount() {
        return amount;
    }

    public long getSeqNo() {
        return seqNo;
    }
}