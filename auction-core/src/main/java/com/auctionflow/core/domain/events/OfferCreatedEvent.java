package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;
import com.auctionflow.core.domain.valueobjects.OfferId;
import com.auctionflow.core.domain.valueobjects.SellerId;

import java.time.Instant;
import java.util.UUID;

public class OfferCreatedEvent extends DomainEvent {
    private final AuctionId auctionId;
    private final BidderId buyerId;
    private final SellerId sellerId;
    private final Money amount;

    public OfferCreatedEvent(OfferId offerId, AuctionId auctionId, BidderId buyerId, SellerId sellerId, Money amount, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(offerId, eventId, timestamp, sequenceNumber);
        this.auctionId = auctionId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
    }

    public AuctionId getAuctionId() {
        return auctionId;
    }

    public BidderId getBuyerId() {
        return buyerId;
    }

    public SellerId getSellerId() {
        return sellerId;
    }

    public Money getAmount() {
        return amount;
    }
}