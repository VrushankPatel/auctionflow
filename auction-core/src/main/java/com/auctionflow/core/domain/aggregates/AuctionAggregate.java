package com.auctionflow.core.domain.aggregates;

import com.auctionflow.core.domain.BidQueue;
import com.auctionflow.core.domain.commands.*;
import com.auctionflow.core.domain.events.*;
import com.auctionflow.core.domain.utils.ObjectPool;
import com.auctionflow.core.domain.validators.BidValidator;
import com.auctionflow.core.domain.validators.ValidationResult;
import java.util.function.Supplier;
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

/**
 * AuctionAggregate handles bid processing for high-frequency auctions.
 * Thread safety: Assumes single-writer per auction instance via sharding on auctionId.
 * Concurrent access to the same auction is not supported and must be prevented at the command bus level.
 */
public class AuctionAggregate extends AggregateRoot {
    // Pool BidValidator instances to reduce allocation in hot paths
    private static final ObjectPool<BidValidator> BID_VALIDATOR_POOL = new ObjectPool<>(10, 100, BidValidator::new);

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
    private final BidQueue bidQueue = new BidQueue();

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
        // Use pooled BidValidator for proper validation with increments to minimize allocation
        BidValidator validator = BID_VALIDATOR_POOL.borrow();
        try {
            ValidationResult result = validator.validate(currentHighestBid, reservePrice, bidIncrement, new BidderId(command.bidderId()), command.amount());
            if (!result.isValid()) {
                throw new IllegalStateException(result.getFirstError());
            }
        } finally {
            BID_VALIDATOR_POOL.release(validator);
        }
        // Use bid queue for efficient processing and ordering with pooled Bid objects
        Bid bid = Bid.create(new BidderId(command.bidderId()), command.amount(), serverTs, seqNo);
        bidQueue.addBid(bid);
        processQueuedBids();
    }

    /**
     * Calculates adaptive batch size based on current queue size to balance latency and throughput.
     * For small queues, use smaller batches to minimize processing latency.
     * For large queues, use larger batches to improve throughput and catch up.
     *
     * @param queueSize current size of the bid queue
     * @return adaptive batch size
     */
    private int calculateAdaptiveBatchSize(int queueSize) {
        if (queueSize <= 5) {
            return Math.min(queueSize, 3); // Process 1-3 bids for small queues
        } else if (queueSize <= 20) {
            return 5; // Moderate batch for medium queues
        } else if (queueSize <= 100) {
            return 10; // Standard batch for larger queues
        } else {
            return 20; // Large batch for very high-frequency scenarios
        }
    }

    /**
     * Processes queued bids in price-time priority order using adaptive batch sizing for balanced latency and throughput.
     * Emits BidPlacedEvent for each bid and checks for reserve met.
     * Adaptive batch size prevents excessive processing in one call while allowing catch-up in high-frequency scenarios.
     */
    private void processQueuedBids() {
        int queueSize = bidQueue.size();
        int maxBatchSize = calculateAdaptiveBatchSize(queueSize); // Adaptive batch size based on queue load
        int processed = 0;
        while (!bidQueue.isEmpty() && processed < maxBatchSize) {
            Bid bid = bidQueue.pollHighestBid();
            if (bid != null) {
                UUID eventId = UUID.randomUUID();
                long sequenceNumber = getVersion() + 1;
                BidPlacedEvent event = new BidPlacedEvent(id, bid.bidderId().id(), bid.amount(), bid.timestamp(), eventId, sequenceNumber, bid.seqNo());
                apply(event);
                addDomainEvent(event);

                // Check if reserve is met for the first time
                if (!reserveMet && bid.amount().isGreaterThanOrEqual(reservePrice)) {
                    reserveMet = true;
                    UUID reserveEventId = UUID.randomUUID();
                    long reserveSequenceNumber = getVersion() + 1;
                    ReserveMetEvent reserveEvent = new ReserveMetEvent(id, bid.bidderId().id(), bid.amount(), reserveEventId, bid.timestamp(), reserveSequenceNumber);
                    addDomainEvent(reserveEvent);
                }
                processed++;
                // Release pooled Bid object after processing
                Bid.release(bid);
            }
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
                    .max(Comparator.comparing((Bid b) -> b.amount().amount(), Comparator.reverseOrder())
                            .thenComparing(Bid::seqNo))
                    .map(bid -> new WinnerId(bid.bidderId().id()))
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
        Bid bid = Bid.create(new BidderId(event.getBidderId()), event.getAmount(), event.getTimestamp(), event.getSeqNo());
        this.bids.add(bid);
        // Price-time priority: higher bid amount takes precedence.
        // For equal amounts, lower sequence number (earlier arrival) wins.
        // This ensures fairness and determinism in high-frequency bidding.
        if (isHigherPriorityBid(event)) {
            this.currentHighestBid = event.getAmount();
            this.highestBidderId = event.getBidderId();
            this.currentSeqNo = event.getSeqNo();
        }
    }

    /**
     * Determines if the incoming bid has higher priority than the current highest.
     * @param event the bid event
     * @return true if this bid should become the new highest
     */
    private boolean isHigherPriorityBid(BidPlacedEvent event) {
        if (this.currentHighestBid == null) {
            return true;
        }
        return event.getAmount().isGreaterThan(this.currentHighestBid) ||
               (event.getAmount().equals(this.currentHighestBid) && event.getSeqNo() < this.currentSeqNo);
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
            // Find the commit seqNo for proper ordering in sealed bids
            Optional<SealedBidCommit> commitOpt = commits.stream()
                    .filter(c -> c.getBidderId().equals(event.getBidderId()))
                    .findFirst();
            long seqNo = commitOpt.map(SealedBidCommit::getSeqNo).orElse(0L);
            Bid bid = Bid.create(event.getBidderId(), event.getAmount(), event.getTimestamp(), seqNo);
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
    /** Returns the bid increment strategy for this auction. */
    public BidIncrement getBidIncrement() { return bidIncrement; }
}