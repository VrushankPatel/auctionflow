package com.auctionflow.events.command;

import com.auctionflow.common.exceptions.OptimisticLockException;
import com.auctionflow.core.domain.aggregates.AggregateRoot;
import com.auctionflow.core.domain.aggregates.AuctionAggregate;
import com.auctionflow.core.domain.aggregates.DutchAuctionAggregate;
import com.auctionflow.core.domain.commands.PlaceBidCommand;
import com.auctionflow.core.domain.events.AuctionCreatedEvent;
import com.auctionflow.core.domain.events.BidPlacedEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.events.ProxyBidOutbidEvent;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.AuctionType;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;
import com.auctionflow.events.AggregateCacheService;
import com.auctionflow.common.service.EventStore;
import com.auctionflow.events.persistence.ProxyBidEntity;
import com.auctionflow.events.persistence.ProxyBidRepository;

import com.auctionflow.events.command.AutomatedBiddingService;
import com.auctionflow.bidding.strategies.BidDecision;
import com.auctionflow.bidding.strategies.StrategyBidDecision;
import io.opentelemetry.extension.annotations.WithSpan;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PlaceBidHandler implements CommandHandler<PlaceBidCommand> {

    private final EventStore eventStore;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final RedissonClient redissonClient;
    private final ProxyBidRepository proxyBidRepository;
    private final AutomatedBiddingService automatedBiddingService;
    private final AggregateCacheService aggregateCacheService;
    // Scheduled executor for non-blocking retries
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(10);

    public PlaceBidHandler(EventStore eventStore, KafkaTemplate<String, DomainEvent> kafkaTemplate, RedissonClient redissonClient, ProxyBidRepository proxyBidRepository, AutomatedBiddingService automatedBiddingService, AggregateCacheService aggregateCacheService) {
        this.eventStore = eventStore;
        this.kafkaTemplate = kafkaTemplate;
        this.redissonClient = redissonClient;
        this.proxyBidRepository = proxyBidRepository;
        this.automatedBiddingService = automatedBiddingService;
        this.aggregateCacheService = aggregateCacheService;
    }

    @Override
    @Async
    @EventListener
    @WithSpan("process-bid-command")
    public void handle(PlaceBidCommand command) {
        String lockKey = "auction:" + command.auctionId().value();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // Try to acquire lock with timeout
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Could not acquire lock for auction " + command.auctionId());
            }
            processBidWithRetry(command, 0, 100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while handling command", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * Generates a globally monotonic sequence number for the auction using Redis atomic increment.
     * Ensures fairness in distributed environments by providing strict ordering.
     */
    private long generateSeqNo(AuctionId auctionId) {
        String key = "auction:seq:" + auctionId.value();
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    private void processBidWithRetry(PlaceBidCommand command, int attempt, long backoffMs) {
        try {
            // Use cached aggregate if available, otherwise reconstruct from events
            AggregateRoot aggregate = aggregateCacheService.get(command.auctionId());
            AuctionType type;
            if (aggregate == null) {
                List<DomainEvent> events = eventStore.getEvents(command.auctionId());
                type = events.stream()
                    .filter(e -> e instanceof AuctionCreatedEvent)
                    .map(e -> ((AuctionCreatedEvent) e).getAuctionType())
                    .findFirst()
                    .orElse(AuctionType.ENGLISH_OPEN);
                if (type == AuctionType.DUTCH) {
                    aggregate = new DutchAuctionAggregate(events);
                } else {
                    aggregate = new AuctionAggregate(events);
                }
            } else {
                // Determine type from cached aggregate
                if (aggregate instanceof DutchAuctionAggregate) {
                    type = AuctionType.DUTCH;
                } else {
                    type = AuctionType.ENGLISH_OPEN;
                }
            }
            aggregate.handle(command);
            List<DomainEvent> newEvents = aggregate.getDomainEvents();
            eventStore.save(newEvents, aggregate.getExpectedVersion());
            // Update cache with new version
            aggregateCacheService.put(command.auctionId(), aggregate);
            // Publish to Kafka in batch
            for (DomainEvent event : newEvents) {
                kafkaTemplate.send("auction-events", event.getAggregateId().toString(), event);
            }

            // Handle proxy bidding after the initial bid is placed asynchronously to reduce latency
            if (type != AuctionType.DUTCH) {
                final AggregateRoot currentAggregate = aggregate; // Capture current state
                retryExecutor.submit(() -> {
                    String lockKey = "auction:proxy:" + command.auctionId().value();
                    RLock lock = redissonClient.getLock(lockKey);
                    try {
                        if (lock.tryLock(5, TimeUnit.SECONDS)) {
                            // Use current aggregate state to avoid reloading events
                            handleProxyBidding(command.auctionId(), currentAggregate);
                            handleAutomatedBidding(command.auctionId(), currentAggregate);
                            // Update cache with updated aggregate
                            aggregateCacheService.put(command.auctionId(), currentAggregate);
                        }
                    } catch (Exception e) {
                        // Log error
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                });
            }



            aggregate.clearDomainEvents();
        } catch (OptimisticLockException e) {
            if (attempt < 3) {
                retryExecutor.schedule(() -> processBidWithRetry(command, attempt + 1, backoffMs * 2), backoffMs, TimeUnit.MILLISECONDS);
            } else {
                throw new RuntimeException("Failed to process bid after retries", e);
            }
        }
    }

    @Transactional
    private void handleProxyBidding(com.auctionflow.core.domain.valueobjects.AuctionId auctionId, AggregateRoot aggregate) {
        // Get the current highest bid from the aggregate
        AuctionAggregate auctionAgg = (AuctionAggregate) aggregate;
        Money currentHighest = auctionAgg.getCurrentHighestBid();

        if (currentHighest == null) {
            return;
        }

        // Find active proxy bids that can bid higher
        List<ProxyBidEntity> proxyBids = proxyBidRepository.findActiveProxyBidsHigherThan(auctionId.value(), currentHighest.toBigDecimal());

        // Collect all events and DB updates for batch processing
        List<DomainEvent> allProxyEvents = new java.util.ArrayList<>();
        List<ProxyBidEntity> toUpdateCurrentBid = new java.util.ArrayList<>();
        List<ProxyBidEntity> toUpdateStatusOutbid = new java.util.ArrayList<>();
        List<ProxyBidEntity> toUpdateStatusWon = new java.util.ArrayList<>();

        for (ProxyBidEntity proxyBid : proxyBids) {
            // Calculate next bid amount using proper increment strategy
            Money nextBidAmount = auctionAgg.getBidIncrement().nextBid(currentHighest);
            if (nextBidAmount.toBigDecimal().compareTo(proxyBid.getMaxBid()) > 0) {
                // Cannot bid, mark as outbid
                toUpdateStatusOutbid.add(proxyBid);
                // Publish outbid event
                ProxyBidOutbidEvent outbidEvent = new ProxyBidOutbidEvent(auctionId, proxyBid.getUserId(), "Maximum bid exceeded", UUID.randomUUID(), Instant.now(), 0);
                allProxyEvents.add(outbidEvent);
                continue;
            }

            // Place the automatic bid
            Instant proxyServerTs = Instant.now();
            long proxySeqNo = generateSeqNo(auctionId);
            PlaceBidCommand proxyCommand = new PlaceBidCommand(auctionId, proxyBid.getUserId(), nextBidAmount, "proxy-" + proxyBid.getId(), proxyServerTs, proxySeqNo);
            // Directly handle the proxy bid on the aggregate
            auctionAgg.handle(proxyCommand);
            List<DomainEvent> proxyEvents = auctionAgg.getDomainEvents();
            allProxyEvents.addAll(proxyEvents);
            auctionAgg.clearDomainEvents();

            // Update proxy bid current bid
            proxyBid.setCurrentBid(nextBidAmount.toBigDecimal());
            toUpdateCurrentBid.add(proxyBid);

            // Update current highest for next iteration
            currentHighest = nextBidAmount;

            // If this proxy bid reached its max, mark as won
            if (nextBidAmount.toBigDecimal().compareTo(proxyBid.getMaxBid()) >= 0) {
                toUpdateStatusWon.add(proxyBid);
            }
        }

        // Batch save all proxy events
        if (!allProxyEvents.isEmpty()) {
            eventStore.save(allProxyEvents, auctionAgg.getExpectedVersion());
            auctionAgg.setExpectedVersion(auctionAgg.getExpectedVersion() + allProxyEvents.size());
            // Batch Kafka sends
            for (DomainEvent event : allProxyEvents) {
                kafkaTemplate.send("auction-events", event.getAggregateId().toString(), event);
            }
        }

        // Batch DB updates
        for (ProxyBidEntity p : toUpdateCurrentBid) {
            proxyBidRepository.updateCurrentBid(p.getId(), p.getCurrentBid());
        }
        for (ProxyBidEntity p : toUpdateStatusOutbid) {
            proxyBidRepository.updateStatus(p.getId(), "OUTBID");
        }
        for (ProxyBidEntity p : toUpdateStatusWon) {
            proxyBidRepository.updateStatus(p.getId(), "WON");
        }
    }

    private void handleAutomatedBidding(com.auctionflow.core.domain.valueobjects.AuctionId auctionId, AggregateRoot aggregate) {
        // Get current highest bid after proxy bidding
        AuctionAggregate auctionAgg = (AuctionAggregate) aggregate;
        Money currentHighest = auctionAgg.getCurrentHighestBid();

        if (currentHighest == null) {
            return;
        }
        Instant auctionEndTime = auctionAgg.getEndTime();

        // Evaluate automated strategies
        List<StrategyBidDecision> decisions = automatedBiddingService.evaluateStrategies(auctionId, currentHighest, auctionEndTime);

        // Collect all events for batch processing
        List<DomainEvent> allAutoEvents = new java.util.ArrayList<>();

        for (StrategyBidDecision strategyDecision : decisions) {
            BidDecision decision = strategyDecision.getDecision();
            if (decision.shouldBid() && decision.getBidTime().isBefore(Instant.now().plusSeconds(1))) { // Only immediate bids for now
                // Place the automated bid for this user
                BidderId bidderId = strategyDecision.getStrategy().getBidderId();
                Instant autoServerTs = Instant.now();
                long autoSeqNo = generateSeqNo(auctionId);
                PlaceBidCommand autoCommand = new PlaceBidCommand(auctionId, bidderId.id(), decision.getBidAmount(), "automated-" + strategyDecision.getStrategy().getId(), autoServerTs, autoSeqNo);

                // Handle the automated bid
                auctionAgg.handle(autoCommand);
                List<DomainEvent> autoEvents = auctionAgg.getDomainEvents();
                allAutoEvents.addAll(autoEvents);
                auctionAgg.clearDomainEvents();

                // Update current highest for next iteration
                currentHighest = decision.getBidAmount();
            }
        }

        // Batch save all automated events
        if (!allAutoEvents.isEmpty()) {
            eventStore.save(allAutoEvents, auctionAgg.getExpectedVersion());
            auctionAgg.setExpectedVersion(auctionAgg.getExpectedVersion() + allAutoEvents.size());
            // Batch Kafka sends
            for (DomainEvent event : allAutoEvents) {
                kafkaTemplate.send("auction-events", event.getAggregateId().toString(), event);
            }
        }
    }
}