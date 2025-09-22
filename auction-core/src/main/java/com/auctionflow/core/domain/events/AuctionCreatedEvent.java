package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.ItemId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;
import java.util.UUID;

public class AuctionCreatedEvent extends DomainEvent {
    private final ItemId itemId;
    private final Money reservePrice;
    private final Money buyNowPrice;
    private final Instant startTime;
    private final Instant endTime;

    public AuctionCreatedEvent(AuctionId auctionId, ItemId itemId, Money reservePrice, Money buyNowPrice, Instant startTime, Instant endTime, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(auctionId, eventId, timestamp, sequenceNumber);
        this.itemId = itemId;
        this.reservePrice = reservePrice;
        this.buyNowPrice = buyNowPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        // other validations as needed
    }

    public ItemId getItemId() {
        return itemId;
    }

    public Money getReservePrice() {
        return reservePrice;
    }

    public Money getBuyNowPrice() {
        return buyNowPrice;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }
}