package com.auctionflow.core.domain.aggregates;

import com.auctionflow.core.domain.commands.*;
import com.auctionflow.core.domain.events.*;
import com.auctionflow.core.domain.valueobjects.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OfferAggregate extends AggregateRoot {
    private OfferId id;
    private AuctionId auctionId;
    private BidderId buyerId;
    private SellerId sellerId;
    private Money amount;
    private OfferStatus status;
    private Instant createdAt;

    public OfferAggregate() {
        this.status = OfferStatus.PENDING;
    }

    public OfferAggregate(List<DomainEvent> events) {
        this();
        for (DomainEvent event : events) {
            if (event instanceof OfferCreatedEvent) {
                apply((OfferCreatedEvent) event);
            } else if (event instanceof OfferAcceptedEvent) {
                apply((OfferAcceptedEvent) event);
            } else if (event instanceof OfferRejectedEvent) {
                apply((OfferRejectedEvent) event);
            }
        }
        this.version = events.size();
        this.expectedVersion = this.version;
    }

    public void handle(MakeOfferCommand command) {
        if (this.id != null) {
            throw new IllegalStateException("Offer already created");
        }
        OfferId offerId = OfferId.generate();
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        long sequenceNumber = getVersion() + 1;
        OfferCreatedEvent event = new OfferCreatedEvent(
            offerId,
            command.auctionId(),
            command.buyerId(),
            command.sellerId(),
            command.amount(),
            eventId,
            timestamp,
            sequenceNumber
        );
        apply(event);
        addDomainEvent(event);
    }

    public void handle(AcceptOfferCommand command) {
        if (status != OfferStatus.PENDING) {
            throw new IllegalStateException("Offer is not pending");
        }
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        long sequenceNumber = getVersion() + 1;
        OfferAcceptedEvent event = new OfferAcceptedEvent(id, eventId, timestamp, sequenceNumber);
        apply(event);
        addDomainEvent(event);
    }

    public void handle(RejectOfferCommand command) {
        if (status != OfferStatus.PENDING) {
            throw new IllegalStateException("Offer is not pending");
        }
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        long sequenceNumber = getVersion() + 1;
        OfferRejectedEvent event = new OfferRejectedEvent(id, eventId, timestamp, sequenceNumber);
        apply(event);
        addDomainEvent(event);
    }

    @EventHandler
    public void apply(OfferCreatedEvent event) {
        this.id = (OfferId) event.getAggregateId();
        this.auctionId = event.getAuctionId();
        this.buyerId = event.getBuyerId();
        this.sellerId = event.getSellerId();
        this.amount = event.getAmount();
        this.createdAt = event.getTimestamp();
    }

    @EventHandler
    public void apply(OfferAcceptedEvent event) {
        this.status = OfferStatus.ACCEPTED;
    }

    @EventHandler
    public void apply(OfferRejectedEvent event) {
        this.status = OfferStatus.REJECTED;
    }

    // Getters
    public OfferId getId() { return id; }
    public AuctionId getAuctionId() { return auctionId; }
    public BidderId getBuyerId() { return buyerId; }
    public SellerId getSellerId() { return sellerId; }
    public Money getAmount() { return amount; }
    public OfferStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public Instant getEndTime() { return null; } // Offers don't have end time
}