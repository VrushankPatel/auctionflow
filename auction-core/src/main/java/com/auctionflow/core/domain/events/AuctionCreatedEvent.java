package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AntiSnipePolicy;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.ItemId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;
import java.util.UUID;

public class AuctionCreatedEvent extends DomainEvent {
    private final ItemId itemId;
    private final String categoryId;
    private final Money reservePrice;
    private final Money buyNowPrice;
    private final Instant startTime;
    private final Instant endTime;
    private final AntiSnipePolicy antiSnipePolicy;

    public AuctionCreatedEvent(AuctionId auctionId, ItemId itemId, String categoryId, Money reservePrice, Money buyNowPrice, Instant startTime, Instant endTime, AntiSnipePolicy antiSnipePolicy, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(auctionId, eventId, timestamp, sequenceNumber);
        this.itemId = itemId;
        this.categoryId = categoryId;
        this.reservePrice = reservePrice;
        this.buyNowPrice = buyNowPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.antiSnipePolicy = antiSnipePolicy;
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

    public AntiSnipePolicy getAntiSnipePolicy() {
        return antiSnipePolicy;
    }

    public String getCategoryId() {
        return categoryId;
    }
}