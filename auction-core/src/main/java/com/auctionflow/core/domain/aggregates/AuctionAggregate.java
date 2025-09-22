package com.auctionflow.core.domain.aggregates;

import com.auctionflow.core.domain.commands.*;
import com.auctionflow.core.domain.events.*;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AuctionAggregate extends AggregateRoot {
    private AuctionId id;
    private ItemId itemId;
    private Money reservePrice;
    private Money buyNowPrice;
    private AuctionStatus status;
    private Instant startTime;
    private Instant endTime;
    private List<Bid> bids;
    private WinnerId winnerId;

    public AuctionAggregate() {
        this.bids = new ArrayList<>();
        this.status = AuctionStatus.CREATED;
    }

    public AuctionAggregate(List<DomainEvent> events) {
        this();
        for (DomainEvent event : events) {
            if (event instanceof AuctionCreatedEvent) {
                apply((AuctionCreatedEvent) event);
            } else if (event instanceof BidPlacedEvent) {
                apply((BidPlacedEvent) event);
            } else if (event instanceof AuctionExtendedEvent) {
                apply((AuctionExtendedEvent) event);
            } else if (event instanceof AuctionClosedEvent) {
                apply((AuctionClosedEvent) event);
            }
        }
        this.version = events.size();
        this.expectedVersion = this.version;
    }

    public void handle(CreateAuctionCommand command) {
        if (this.id != null) {
            throw new IllegalStateException("Auction already created");
        }
        AuctionId auctionId = AuctionId.generate();
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        long sequenceNumber = getVersion() + 1;
        AuctionCreatedEvent event = new AuctionCreatedEvent(
            auctionId,
            command.itemId(),
            command.reservePrice(),
            command.buyNowPrice(),
            command.startTime(),
            command.endTime(),
            eventId,
            timestamp,
            sequenceNumber
        );
        apply(event);
        addDomainEvent(event);
    }

    public void handle(PlaceBidCommand command) {
        if (status != AuctionStatus.OPEN) {
            throw new IllegalStateException("Auction is not open for bidding");
        }
        Instant now = Instant.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            throw new IllegalStateException("Auction is not active");
        }
        Optional<Bid> highestBid = bids.stream().max(Comparator.comparing(Bid::amount));
        if (highestBid.isPresent() && !command.amount().isGreaterThan(highestBid.get().amount())) {
            throw new IllegalStateException("Bid must be higher than current highest bid");
        }
        if (!command.amount().isGreaterThanOrEqual(reservePrice)) {
            throw new IllegalStateException("Bid must meet reserve price");
        }
        Bid bid = new Bid(command.bidderId(), command.amount(), now);
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = getVersion() + 1;
        BidPlacedEvent event = new BidPlacedEvent(id, command.bidderId(), command.amount(), now, eventId, sequenceNumber);
        apply(event);
        addDomainEvent(event);
    }

    public void handle(ExtendAuctionCommand command) {
        if (status != AuctionStatus.OPEN) {
            throw new IllegalStateException("Auction is not open");
        }
        if (command.newEndTime().isBefore(endTime)) {
            throw new IllegalStateException("New end time must be after current end time");
        }
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        long sequenceNumber = getVersion() + 1;
        AuctionExtendedEvent event = new AuctionExtendedEvent(id, command.newEndTime(), eventId, timestamp, sequenceNumber);
        apply(event);
        addDomainEvent(event);
    }

    public void handle(CloseAuctionCommand command) {
        if (status != AuctionStatus.OPEN) {
            throw new IllegalStateException("Auction is not open");
        }
        Instant now = Instant.now();
        if (now.isBefore(endTime)) {
            throw new IllegalStateException("Auction has not ended yet");
        }
        Optional<Bid> highestBid = bids.stream().max(Comparator.comparing(Bid::amount));
        WinnerId winner = highestBid.map(bid -> new WinnerId(bid.bidderId())).orElse(null);
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = getVersion() + 1;
        AuctionClosedEvent event = new AuctionClosedEvent(id, winner, eventId, now, sequenceNumber);
        apply(event);
        addDomainEvent(event);
    }

    @EventHandler
    public void apply(AuctionCreatedEvent event) {
        this.id = event.getAggregateId();
        this.itemId = event.getItemId();
        this.reservePrice = event.getReservePrice();
        this.buyNowPrice = event.getBuyNowPrice();
        this.startTime = event.getStartTime();
        this.endTime = event.getEndTime();
        this.status = AuctionStatus.OPEN;
    }

    @EventHandler
    public void apply(BidPlacedEvent event) {
        Bid bid = new Bid(event.getBidderId(), event.getAmount(), event.getTimestamp());
        this.bids.add(bid);
    }

    @EventHandler
    public void apply(AuctionExtendedEvent event) {
        this.endTime = event.getNewEndTime();
    }

    @EventHandler
    public void apply(AuctionClosedEvent event) {
        this.status = AuctionStatus.CLOSED;
        this.winnerId = event.getWinnerId();
    }

    // Getters for testing or external access
    public AuctionId getId() { return id; }
    public AuctionStatus getStatus() { return status; }
    public List<Bid> getBids() { return new ArrayList<>(bids); }
    public WinnerId getWinnerId() { return winnerId; }
}