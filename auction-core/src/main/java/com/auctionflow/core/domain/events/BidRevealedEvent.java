package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;
import java.util.UUID;

public class BidRevealedEvent extends DomainEvent {
    private final AuctionId auctionId;
    private final BidderId bidderId;
    private final Money amount;
    private final String salt;
    private final boolean valid;

    public BidRevealedEvent(AuctionId auctionId, BidderId bidderId, Money amount, String salt, boolean valid,
                            UUID eventId, Instant timestamp, long sequenceNumber) {
        super(auctionId, eventId, timestamp, sequenceNumber);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.salt = salt;
        this.valid = valid;
    }

    public AuctionId getAuctionId() {
        return auctionId;
    }

    public BidderId getBidderId() {
        return bidderId;
    }

    public Money getAmount() {
        return amount;
    }

    public String getSalt() {
        return salt;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public AuctionId getAggregateId() {
        return auctionId;
    }
}