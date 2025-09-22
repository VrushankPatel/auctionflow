package com.auctionflow.core.domain.aggregates;

import com.auctionflow.core.domain.commands.*;
import com.auctionflow.core.domain.events.*;
import com.auctionflow.core.domain.validators.BidValidator;
import com.auctionflow.core.domain.validators.ValidationResult;
import com.auctionflow.core.domain.valueobjects.*;
import com.auctionflow.core.domain.valueobjects.FixedBidIncrement;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AuctionAggregate extends AggregateRoot {
    private AuctionId id;
    private ItemId itemId;
    private AuctionType auctionType;
    private Money reservePrice;
    private Money buyNowPrice;
    private boolean hiddenReserve;
    private boolean reserveMet;
    private AuctionStatus status;
    private Instant startTime;
    private Instant endTime;
    private Duration originalDuration;
    private AntiSnipePolicy antiSnipePolicy;
    private long extensionsCount;
    private List<Bid> bids;
    private List<SealedBidCommit> commits;
    private List<Bid> revealedBids;
    private WinnerId winnerId;
    private Money currentHighestBid;
    private UUID highestBidderId;
    private BidIncrement bidIncrement;
    private long currentSeqNo;

    public AuctionAggregate() {
        this.bids = new ArrayList<>();
        this.commits = new ArrayList<>();
        this.revealedBids = new ArrayList<>();
        this.status = AuctionStatus.CREATED;
        this.extensionsCount = 0;
        this.currentHighestBid = null;
        this.highestBidderId = null;
        this.bidIncrement = new FixedBidIncrement(new Money(BigDecimal.ONE));
        this.currentSeqNo = Long.MAX_VALUE;
    }

    public AuctionAggregate(List<DomainEvent> events) {
        this();
        for (DomainEvent event : events) {
            apply(event);
        }
        this.version = events.size();
        this.expectedVersion = this.version;
    }

    public void apply(DomainEvent event) {
        if (event instanceof AuctionCreatedEvent) {
            apply((AuctionCreatedEvent) event);
        } else if (event instanceof BidPlacedEvent) {
            apply((BidPlacedEvent) event);
        } else if (event instanceof BidCommittedEvent) {
            apply((BidCommittedEvent) event);
        } else if (event instanceof BidRevealedEvent) {
            apply((BidRevealedEvent) event);
        } else if (event instanceof AuctionRevealPhaseStartedEvent) {
            apply((AuctionRevealPhaseStartedEvent) event);
        } else if (event instanceof AuctionExtendedEvent) {
            apply((AuctionExtendedEvent) event);
        } else if (event instanceof AuctionClosedEvent) {
            apply((AuctionClosedEvent) event);
        } else if (event instanceof ReserveMetEvent) {
            apply((ReserveMetEvent) event);
        }
        // Add more event types as needed
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
            command.categoryId(),
            command.auctionType(),
            command.reservePrice(),
            command.buyNowPrice(),
            command.startTime(),
            command.endTime(),
            command.antiSnipePolicy(),
            command.hiddenReserve(),
            eventId,
            timestamp,
            sequenceNumber
        );
        apply(event);
        addDomainEvent(event);
    }

    public void handle(StartRevealPhaseCommand command) {
        if (auctionType != AuctionType.SEALED_BID) {
            throw new IllegalStateException("Reveal phase only for sealed auctions");
        }
        if (status != AuctionStatus.SEALED_BIDDING) {
            throw new IllegalStateException("Auction is not in sealed bidding phase");
        }
        Instant now = Instant.now();
        if (now.isBefore(endTime)) {
            throw new IllegalStateException("Bidding phase has not ended yet");
        }
        // Set reveal phase end time, e.g., 1 hour after bidding ends
        Instant revealEndTime = endTime.plusSeconds(3600);
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = getVersion() + 1;
        AuctionRevealPhaseStartedEvent event = new AuctionRevealPhaseStartedEvent(id, revealEndTime,
                                                                                  eventId, now, sequenceNumber);
        apply(event);
        addDomainEvent(event);
    }

    public void handle(RevealBidCommand command) {
        if (auctionType != AuctionType.SEALED_BID) {
            throw new IllegalStateException("Reveal bid only for sealed auctions");
        }
        if (status != AuctionStatus.REVEAL_PHASE) {
            throw new IllegalStateException("Auction is not in reveal phase");
        }
        // Find the commit for this bidder
        Optional<SealedBidCommit> commitOpt = commits.stream()
                .filter(c -> c.getBidderId().equals(command.bidderId()))
                .findFirst();
        if (commitOpt.isEmpty()) {
            throw new IllegalStateException("No commit found for bidder");
        }
        SealedBidCommit commit = commitOpt.get();
        // Verify the hash
        boolean valid = com.auctionflow.core.domain.utils.CryptoUtils.verifyBid(
                command.amount().toString(), command.salt(), commit.getHash());
        Instant now = Instant.now();
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = getVersion() + 1;
        BidRevealedEvent event = new BidRevealedEvent(id, command.bidderId(), command.amount(), command.salt(), valid,
                                                      eventId, now, sequenceNumber);
        apply(event);
        addDomainEvent(event);
    }

    public void handle(CommitBidCommand command) {
        if (auctionType != AuctionType.SEALED_BID) {
            throw new IllegalStateException("Commit bid only for sealed auctions");
        }
        if (status != AuctionStatus.SEALED_BIDDING) {
            throw new IllegalStateException("Auction is not in sealed bidding phase");
        }
        Instant now = Instant.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            throw new IllegalStateException("Auction is not active");
        }
        // Check if bidder already committed
        boolean alreadyCommitted = commits.stream().anyMatch(c -> c.getBidderId().equals(command.bidderId()));
        if (alreadyCommitted) {
            throw new IllegalStateException("Bidder has already committed a bid");
        }
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = getVersion() + 1;
        BidCommittedEvent event = new BidCommittedEvent(id, command.bidderId(), command.bidHash(), command.salt(),
                                                        sequenceNumber, eventId, now, sequenceNumber);
        apply(event);
        addDomainEvent(event);
    }

    /**
     * Handles placing a bid on the auction.
     * Validates bid amount using BidValidator with increments, updates state with price-time priority, and emits events.
     * Optimized for high-frequency bidding with server-assigned timestamps and sequence numbers for fairness.
     * @param command the place bid command containing server timestamp and sequence number
     */
    public void handle(PlaceBidCommand command) {
        if (auctionType == AuctionType.SEALED_BID) {
            throw new IllegalStateException("Use commit bid for sealed auctions");
        }
        Instant serverTs = command.serverTs();
        long seqNo = command.seqNo();
        if (status != AuctionStatus.OPEN) {
            throw new IllegalStateException("Auction is not open for bidding");
        }
        if (serverTs.isBefore(startTime) || serverTs.isAfter(endTime)) {
            throw new IllegalStateException("Auction is not active");
        }
        // Use BidValidator for proper validation with increments
        BidValidator validator = new BidValidator();
        ValidationResult result = validator.validate(currentHighestBid, reservePrice, bidIncrement, new BidderId(command.bidderId()), command.amount());
        if (!result.isValid()) {
            throw new IllegalStateException(result.getErrors().get(0));
        }
        // TODO: Use object pool for Bid and event objects to achieve zero-allocation in hot paths
        Bid bid = new Bid(new BidderId(command.bidderId()), command.amount(), serverTs, seqNo);
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = getVersion() + 1;
        BidPlacedEvent event = new BidPlacedEvent(id, command.bidderId(), command.amount(), serverTs, eventId, sequenceNumber, seqNo);
        apply(event);
        addDomainEvent(event);

        // Check if reserve is met for the first time
        if (!reserveMet && command.amount().isGreaterThanOrEqual(reservePrice)) {
            reserveMet = true;
            UUID reserveEventId = UUID.randomUUID();
            long reserveSequenceNumber = getVersion() + 1;
            ReserveMetEvent reserveEvent = new ReserveMetEvent(id, command.bidderId(), command.amount(), reserveEventId, serverTs, reserveSequenceNumber);
            addDomainEvent(reserveEvent);
        }
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
        if (status != AuctionStatus.OPEN && status != AuctionStatus.REVEAL_PHASE) {
            throw new IllegalStateException("Auction is not open or in reveal phase");
        }
        Instant now = Instant.now();
        if (now.isBefore(endTime)) {
            throw new IllegalStateException("Auction has not ended yet");
        }
        WinnerId winner = null;
        if (auctionType == AuctionType.SEALED_BID) {
            // For sealed bid, winner from revealed bids: higher amount, then lower seqNo (earlier commit)
            winner = revealedBids.stream()
                    .max(Comparator.comparing(Bid::amount)
                            .thenComparing(Bid::seqNo))
                    .map(bid -> new WinnerId(bid.bidderId()))
                    .orElse(null);
        } else {
            winner = highestBidderId != null ? new WinnerId(highestBidderId) : null;
        }
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
        this.auctionType = event.getAuctionType();
        this.reservePrice = event.getReservePrice();
        this.buyNowPrice = event.getBuyNowPrice();
        this.hiddenReserve = event.isHiddenReserve();
        this.reserveMet = false;
        this.startTime = event.getStartTime();
        this.endTime = event.getEndTime();
        this.originalDuration = Duration.between(event.getStartTime(), event.getEndTime());
        this.antiSnipePolicy = event.getAntiSnipePolicy();
        this.status = event.getAuctionType() == AuctionType.SEALED_BID ? AuctionStatus.SEALED_BIDDING : AuctionStatus.OPEN;
        this.currentHighestBid = event.getReservePrice();
        this.bidIncrement = new FixedBidIncrement(new Money(BigDecimal.ONE));
        this.currentSeqNo = Long.MAX_VALUE;
    }

    @EventHandler
    public void apply(BidPlacedEvent event) {
        Bid bid = new Bid(new BidderId(event.getBidderId()), event.getAmount(), event.getTimestamp(), event.getSeqNo());
        this.bids.add(bid);
        // Implement price-time priority: higher amount wins, or same amount with lower seqNo (earlier)
        if (this.currentHighestBid == null || event.getAmount().isGreaterThan(this.currentHighestBid) ||
            (event.getAmount().equals(this.currentHighestBid) && event.getSeqNo() < this.currentSeqNo)) {
            this.currentHighestBid = event.getAmount();
            this.highestBidderId = event.getBidderId();
            this.currentSeqNo = event.getSeqNo();
        }
    }

    @EventHandler
    public void apply(BidCommittedEvent event) {
        SealedBidCommit commit = new SealedBidCommit(event.getBidderId(), event.getBidHash(),
                                                     event.getSalt(), event.getTimestamp(), event.getCommitSeqNo());
        this.commits.add(commit);
    }

    @EventHandler
    public void apply(BidRevealedEvent event) {
        if (event.isValid()) {
            Bid bid = new Bid(event.getBidderId(), event.getAmount(), event.getTimestamp());
            this.revealedBids.add(bid);
        }
    }

    @EventHandler
    public void apply(AuctionRevealPhaseStartedEvent event) {
        this.status = AuctionStatus.REVEAL_PHASE;
        this.endTime = event.getRevealEndTime();
    }

    @EventHandler
    public void apply(AuctionExtendedEvent event) {
        this.endTime = event.getNewEndTime();
        this.extensionsCount++;
    }

    @EventHandler
    public void apply(AuctionClosedEvent event) {
        this.status = AuctionStatus.CLOSED;
        this.winnerId = event.getWinnerId();
    }

    @EventHandler
    public void apply(ReserveMetEvent event) {
        this.reserveMet = true;
    }

    // Getters for testing or external access
    public AuctionId getId() { return id; }
    public AuctionType getAuctionType() { return auctionType; }
    public AuctionStatus getStatus() { return status; }
    public Instant getEndTime() { return endTime; }
    public Duration getOriginalDuration() { return originalDuration; }
    public AntiSnipePolicy getAntiSnipePolicy() { return antiSnipePolicy; }
    public long getExtensionsCount() { return extensionsCount; }
    public List<Bid> getBids() { return new ArrayList<>(bids); }
    /** Returns the current highest bid amount, or null if no bids. */
    public Money getCurrentHighestBid() { return currentHighestBid; }
    public WinnerId getWinnerId() { return winnerId; }
}