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
import com.auctionflow.core.domain.valueobjects.Money;
import com.auctionflow.events.EventStore;
import com.auctionflow.events.persistence.ProxyBidEntity;
import com.auctionflow.events.persistence.ProxyBidRepository;
import com.auctionflow.timers.AntiSnipeExtension;
import com.auctionflow.bidding.strategies.AutomatedBiddingService;
import com.auctionflow.bidding.strategies.BidDecision;
import com.auctionflow.bidding.strategies.StrategyBidDecision;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PlaceBidHandler implements CommandHandler<PlaceBidCommand> {

    private final EventStore eventStore;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final RedissonClient redissonClient;
    private final AntiSnipeExtension antiSnipeExtension;
    private final ProxyBidRepository proxyBidRepository;
    private final AutomatedBiddingService automatedBiddingService;
    // In-memory cache for aggregates to avoid reconstruction on every bid
    private final Cache<AuctionId, AggregateRoot> aggregateCache = Caffeine.newBuilder()
            .maximumSize(10000) // Adjust based on active auctions
            .expireAfterWrite(10, TimeUnit.MINUTES) // Expire after inactivity
            .build();
    // Scheduled executor for non-blocking retries
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(10);

    public PlaceBidHandler(EventStore eventStore, KafkaTemplate<String, DomainEvent> kafkaTemplate, RedissonClient redissonClient, AntiSnipeExtension antiSnipeExtension, ProxyBidRepository proxyBidRepository, AutomatedBiddingService automatedBiddingService) {
        this.eventStore = eventStore;
        this.kafkaTemplate = kafkaTemplate;
        this.redissonClient = redissonClient;
        this.antiSnipeExtension = antiSnipeExtension;
        this.proxyBidRepository = proxyBidRepository;
        this.automatedBiddingService = automatedBiddingService;
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
            AggregateRoot aggregate = aggregateCache.getIfPresent(command.auctionId());
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
            aggregateCache.put(command.auctionId(), aggregate);
            // Publish to Kafka
            for (DomainEvent event : newEvents) {
                kafkaTemplate.send("auction-events", event.getAggregateId().toString(), event);
            }

            // Handle proxy bidding after the initial bid is placed asynchronously to reduce latency
            if (type != AuctionType.DUTCH) {
                retryExecutor.submit(() -> {
                    String lockKey = "auction:proxy:" + command.auctionId().value();
                    RLock lock = redissonClient.getLock(lockKey);
                    try {
                        if (lock.tryLock(5, TimeUnit.SECONDS)) {
                            // Reload aggregate to get latest state
                            List<DomainEvent> events = eventStore.getEvents(command.auctionId());
                            AggregateRoot freshAggregate = type == AuctionType.DUTCH ? new DutchAuctionAggregate(events) : new AuctionAggregate(events);
                            handleProxyBidding(command.auctionId(), freshAggregate);
                            handleAutomatedBidding(command.auctionId(), freshAggregate);
                            // Update cache
                            aggregateCache.put(command.auctionId(), freshAggregate);
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

            // Check for anti-snipe extension (only for English)
            if (type != AuctionType.DUTCH) {
                Instant bidTime = command.serverTs(); // Use precise server timestamp from command
                AuctionAggregate english = (AuctionAggregate) aggregate;
                antiSnipeExtension.applyExtensionIfNeeded(
                    english.getId(),
                    bidTime,
                    english.getEndTime(),
                    english.getOriginalDuration(),
                    english.getAntiSnipePolicy(),
                    english.getExtensionsCount()
                );
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

        for (ProxyBidEntity proxyBid : proxyBids) {
            // Calculate next bid amount using proper increment strategy
            AuctionAggregate auctionAgg = (AuctionAggregate) aggregate;
            Money nextBidAmount = auctionAgg.getBidIncrement().nextBid(currentHighest);
            if (nextBidAmount.toBigDecimal().compareTo(proxyBid.getMaxBid()) > 0) {
                // Cannot bid, mark as outbid
                proxyBidRepository.updateStatus(proxyBid.getId(), "OUTBID");
                // Publish outbid event
                ProxyBidOutbidEvent outbidEvent = new ProxyBidOutbidEvent(auctionId, proxyBid.getUserId(), "Maximum bid exceeded", UUID.randomUUID(), Instant.now(), 0); // sequence not used
                kafkaTemplate.send("auction-events", outbidEvent.getAggregateId().toString(), outbidEvent);
                continue;
            }

            // Place the automatic bid
            Instant proxyServerTs = Instant.now();
            long proxySeqNo = generateSeqNo(auctionId);
            PlaceBidCommand proxyCommand = new PlaceBidCommand(auctionId, proxyBid.getUserId(), nextBidAmount, "proxy-" + proxyBid.getId(), proxyServerTs, proxySeqNo);
            // Directly handle the proxy bid on the aggregate
            auctionAgg.handle(proxyCommand);
            List<DomainEvent> proxyEvents = auctionAgg.getDomainEvents();
            long newExpectedVersion = auctionAgg.getExpectedVersion() + proxyEvents.size();
            eventStore.save(proxyEvents, auctionAgg.getExpectedVersion());
            auctionAgg.setExpectedVersion(newExpectedVersion);
            for (DomainEvent event : proxyEvents) {
                kafkaTemplate.send("auction-events", event.getAggregateId().toString(), event);
            }
            auctionAgg.clearDomainEvents();

            // Update proxy bid current bid
            proxyBidRepository.updateCurrentBid(proxyBid.getId(), nextBidAmount.toBigDecimal());

            // Update current highest for next iteration
            currentHighest = nextBidAmount;

            // If this proxy bid reached its max, mark as won or active
            if (nextBidAmount.toBigDecimal().compareTo(proxyBid.getMaxBid()) >= 0) {
                proxyBidRepository.updateStatus(proxyBid.getId(), "WON");
            }
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

        for (StrategyBidDecision strategyDecision : decisions) {
            BidDecision decision = strategyDecision.getDecision();
            if (decision.shouldBid() && decision.getBidTime().isBefore(Instant.now().plusSeconds(1))) { // Only immediate bids for now
                // Place the automated bid for this user
                BidderId bidderId = BidderId.fromString(strategyDecision.getStrategy().getBidderId());
                Instant autoServerTs = Instant.now();
                long autoSeqNo = generateSeqNo(auctionId);
                PlaceBidCommand autoCommand = new PlaceBidCommand(auctionId, bidderId, decision.getBidAmount(), "automated-" + strategyDecision.getStrategy().getId(), autoServerTs, autoSeqNo);

                // Handle the automated bid
                auctionAgg.handle(autoCommand);
                List<DomainEvent> autoEvents = auctionAgg.getDomainEvents();
                long newExpectedVersion = auctionAgg.getExpectedVersion() + autoEvents.size();
                eventStore.save(autoEvents, auctionAgg.getExpectedVersion());
                auctionAgg.setExpectedVersion(newExpectedVersion);
                for (DomainEvent event : autoEvents) {
                    kafkaTemplate.send("auction-events", event.getAggregateId().toString(), event);
                }
                auctionAgg.clearDomainEvents();

                // Update current highest for next iteration
                currentHighest = decision.getBidAmount();
            }
        }
    }
}