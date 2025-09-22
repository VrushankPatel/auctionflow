package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.time.Instant;
import java.util.UUID;

public class ProxyBidOutbidEvent extends DomainEvent {

    private final AuctionId auctionId;
    private final UUID userId;
    private final String reason;

    public ProxyBidOutbidEvent(AuctionId auctionId, UUID userId, String reason, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(auctionId, eventId, timestamp, sequenceNumber);
        this.auctionId = auctionId;
        this.userId = userId;
        this.reason = reason;
    }

    public AuctionId getAuctionId() {
        return auctionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getReason() {
        return reason;
    }
}