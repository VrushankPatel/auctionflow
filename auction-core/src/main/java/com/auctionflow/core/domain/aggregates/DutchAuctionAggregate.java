package com.auctionflow.core.domain.aggregates;

import com.auctionflow.core.domain.commands.*;
import com.auctionflow.core.domain.events.*;
import com.auctionflow.core.domain.valueobjects.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DutchAuctionAggregate extends AggregateRoot {
    private AuctionId id;
    private ItemId itemId;
    private AuctionType auctionType;
    private Money startingPrice;
    private Money currentPrice;
    private DutchAuctionRules rules;
    private AuctionStatus status;
    private Instant startTime;
    private Instant endTime;
    private WinnerId winnerId;
    private List<PriceHistoryEntry> priceHistory;

    public DutchAuctionAggregate() {
        this.auctionType = AuctionType.DUTCH;
        this.status = AuctionStatus.CREATED;
        this.priceHistory = new ArrayList<>();
    }

    public DutchAuctionAggregate(List<DomainEvent> events) {
        this();
        for (DomainEvent event : events) {
            if (event instanceof AuctionCreatedEvent) {
                apply((AuctionCreatedEvent) event);
            } else if (event instanceof PriceReducedEvent) {
                apply((PriceReducedEvent) event);
            } else if (event instanceof BidPlacedEvent) {
                apply((BidPlacedEvent) event);
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
        if (command.auctionType() != AuctionType.DUTCH) {
            throw new IllegalStateException("This aggregate is for Dutch auctions only");
        }
        AuctionId auctionId = AuctionId.generate();
        // Assume reservePrice is startingPrice, buyNowPrice is minimumPrice
        Money startingPrice = command.reservePrice();
        Money minimumPrice = command.buyNowPrice();
        // For simplicity, assume decrementAmount is some fixed, say 1% or something, but need to define.
        // Perhaps need to add to command, but for now, hardcode or assume.
        // To make it simple, let's assume decrementAmount is (starting - min)/10 or something.
        // But better to add to rules.
        // For now, create rules with minimumPrice, and assume decrementInterval 1 minute, decrementAmount 10.
        // But this is hacky. Perhaps update CreateAuctionCommand to include DutchAuctionRules if type is DUTCH.
        // But to keep simple, assume reservePrice > buyNowPrice, decrementAmount = (reserve - buyNow)/10, interval 1 min.
        Money decrementAmount = startingPrice.subtract(minimumPrice).divide(10);
        Duration decrementInterval = Duration.ofMinutes(1);
        DutchAuctionRules rules = new DutchAuctionRules(minimumPrice, decrementAmount, decrementInterval);

        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        long sequenceNumber = getVersion() + 1;
        AuctionCreatedEvent event = new AuctionCreatedEvent(
            auctionId,
            command.itemId(),
            command.categoryId(),
            command.auctionType(),
            command.reservePrice(),
            command.buyNowPrice(),
            command.startTime(),
            command.endTime(),
            command.antiSnipePolicy(),
            eventId,
            timestamp,
            sequenceNumber
        );
        apply(event);
        addDomainEvent(event);
    }

    public void handle(PlaceBidCommand command, Instant serverTs, long seqNo) {
        if (status != AuctionStatus.OPEN) {
            throw new IllegalStateException("Auction is not open for bidding");
        }
        if (serverTs.isBefore(startTime) || serverTs.isAfter(endTime)) {
            throw new IllegalStateException("Auction is not active");
        }
        // In Dutch, bid means accept current price, instant purchase
        if (!command.amount().equals(currentPrice)) {
            throw new IllegalStateException("Bid amount must match current price");
        }
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = getVersion() + 1;
        BidPlacedEvent event = new BidPlacedEvent(id, command.bidderId(), currentPrice, serverTs, eventId, sequenceNumber, seqNo);
        apply(event);
        addDomainEvent(event);
    }

    public void handle(ReducePriceCommand command) {
        if (status != AuctionStatus.OPEN) {
            throw new IllegalStateException("Auction is not open");
        }
        Instant now = Instant.now();
        if (now.isAfter(endTime)) {
            throw new IllegalStateException("Auction has ended");
        }
        Money newPrice = currentPrice.subtract(rules.decrementAmount());
        if (newPrice.isLessThan(rules.minimumPrice())) {
            newPrice = rules.minimumPrice();
        }
        if (newPrice.equals(currentPrice)) {
            // No reduction needed
            return;
        }
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = getVersion() + 1;
        PriceReducedEvent event = new PriceReducedEvent(id, newPrice, eventId, now, sequenceNumber);
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
        // If no winner, winnerId remains null
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = getVersion() + 1;
        AuctionClosedEvent event = new AuctionClosedEvent(id, winnerId, eventId, now, sequenceNumber);
        apply(event);
        addDomainEvent(event);
    }

    @EventHandler
    public void apply(AuctionCreatedEvent event) {
        this.id = event.getAggregateId();
        this.itemId = event.getItemId();
        this.auctionType = event.getAuctionType();
        this.startingPrice = event.getReservePrice();
        this.currentPrice = event.getReservePrice();
        this.status = AuctionStatus.OPEN;
        this.startTime = event.getStartTime();
        this.endTime = event.getEndTime();
        // Rules as above
        Money minimumPrice = event.getBuyNowPrice();
        Money decrementAmount = startingPrice.subtract(minimumPrice).divide(10);
        Duration decrementInterval = Duration.ofMinutes(1);
        this.rules = new DutchAuctionRules(minimumPrice, decrementAmount, decrementInterval);
        this.priceHistory.add(new PriceHistoryEntry(currentPrice, startTime));
    }

    @EventHandler
    public void apply(PriceReducedEvent event) {
        this.currentPrice = event.getNewPrice();
        this.priceHistory.add(new PriceHistoryEntry(currentPrice, event.getTimestamp()));
    }

    @EventHandler
    public void apply(BidPlacedEvent event) {
        this.winnerId = new WinnerId(event.getBidderId());
        this.status = AuctionStatus.CLOSED;
    }

    @EventHandler
    public void apply(AuctionClosedEvent event) {
        this.status = AuctionStatus.CLOSED;
        this.winnerId = event.getWinnerId();
    }

    // Getters
    public AuctionId getId() { return id; }
    public AuctionType getAuctionType() { return auctionType; }
    public AuctionStatus getStatus() { return status; }
    public Money getCurrentPrice() { return currentPrice; }
    public DutchAuctionRules getRules() { return rules; }
    public Instant getEndTime() { return endTime; }
    public WinnerId getWinnerId() { return winnerId; }
    public List<PriceHistoryEntry> getPriceHistory() { return new ArrayList<>(priceHistory); }

    public static class PriceHistoryEntry {
        private final Money price;
        private final Instant timestamp;

        public PriceHistoryEntry(Money price, Instant timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }

        public Money getPrice() { return price; }
        public Instant getTimestamp() { return timestamp; }
    }
}