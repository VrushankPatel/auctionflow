package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;
import java.util.UUID;

public class ReserveMetEvent extends DomainEvent {
    private final BidderId bidderId;
    private final Money bidAmount;

    public ReserveMetEvent(AuctionId auctionId, BidderId bidderId, Money bidAmount, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(auctionId, eventId, timestamp, sequenceNumber);
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
    }

    public BidderId getBidderId() {
        return bidderId;
    }

    public Money getBidAmount() {
        return bidAmount;
    }
}