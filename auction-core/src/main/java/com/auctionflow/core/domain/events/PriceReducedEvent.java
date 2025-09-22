package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;
import java.util.UUID;

public class PriceReducedEvent extends DomainEvent {
    private final Money newPrice;

    public PriceReducedEvent(AuctionId auctionId, Money newPrice, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(auctionId, eventId, timestamp, sequenceNumber);
        this.newPrice = newPrice;
        if (newPrice == null) {
            throw new IllegalArgumentException("NewPrice cannot be null");
        }
    }

    public Money getNewPrice() {
        return newPrice;
    }
}