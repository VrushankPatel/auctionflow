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
import com.auctionflow.core.domain.valueobjects.AuctionType;
import com.auctionflow.core.domain.valueobjects.Money;
import com.auctionflow.events.EventStore;
import com.auctionflow.events.persistence.ProxyBidEntity;
import com.auctionflow.events.persistence.ProxyBidRepository;
import com.auctionflow.timers.AntiSnipeExtension;
import com.auctionflow.bidding.strategies.AutomatedBiddingService;
import com.auctionflow.bidding.strategies.BidDecision;
import com.auctionflow.bidding.strategies.StrategyBidDecision;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PlaceBidHandler implements CommandHandler<PlaceBidCommand> {

    private final AtomicLong sequenceGenerator = new AtomicLong(0);
    private final EventStore eventStore;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final RedissonClient redissonClient;
    private final AntiSnipeExtension antiSnipeExtension;
    private final ProxyBidRepository proxyBidRepository;
    private final AutomatedBiddingService automatedBiddingService;

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
            int maxRetries = 3;
            long backoffMs = 100;
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
            Instant serverTs = Instant.now();
            long seqNo = sequenceGenerator.incrementAndGet();
            List<DomainEvent> events = eventStore.getEvents(command.auctionId());
                     AuctionType type = events.stream()
                         .filter(e -> e instanceof AuctionCreatedEvent)
                         .map(e -> ((AuctionCreatedEvent) e).getAuctionType())
                         .findFirst()
                         .orElse(AuctionType.ENGLISH_OPEN); // default for old auctions
                     AggregateRoot aggregate;
                     if (type == AuctionType.DUTCH) {
                         DutchAuctionAggregate dutch = new DutchAuctionAggregate(events);
                         dutch.handle(command, serverTs, seqNo);
                         aggregate = dutch;
                     } else {
                         AuctionAggregate english = new AuctionAggregate(events);
                         english.handle(command, serverTs, seqNo);
                         aggregate = english;
                     }
                    List<DomainEvent> newEvents = aggregate.getDomainEvents();
                    eventStore.save(newEvents, aggregate.getExpectedVersion());
                    // Publish to Kafka
                    for (DomainEvent event : newEvents) {
                        kafkaTemplate.send("auction-events", event.getAggregateId().toString(), event);
                    }

                    // Handle proxy bidding after the initial bid is placed
                    if (type != AuctionType.DUTCH) {
                        handleProxyBidding(command.auctionId(), aggregate);
                        handleAutomatedBidding(command.auctionId(), aggregate);
                    }

                    // Check for anti-snipe extension (only for English)
                    if (type != AuctionType.DUTCH) {
                        Instant bidTime = Instant.now(); // Approximate, since it's after handling
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
                    break;
                } catch (OptimisticLockException e) {
                    if (attempt == maxRetries) {
                        throw e;
                    }
                    // Backoff
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while handling command", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
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
            // Calculate next bid amount (simple increment for now)
            Money nextBidAmount = currentHighest.add(new Money(BigDecimal.ONE));
            if (nextBidAmount.toBigDecimal().compareTo(proxyBid.getMaxBid()) > 0) {
                // Cannot bid, mark as outbid
                proxyBidRepository.updateStatus(proxyBid.getId(), "OUTBID");
                // Publish outbid event
                ProxyBidOutbidEvent outbidEvent = new ProxyBidOutbidEvent(auctionId, proxyBid.getUserId(), "Maximum bid exceeded", UUID.randomUUID(), Instant.now(), 0); // sequence not used
                kafkaTemplate.send("auction-events", outbidEvent.getAggregateId().toString(), outbidEvent);
                continue;
            }

            // Place the automatic bid
            PlaceBidCommand proxyCommand = new PlaceBidCommand(auctionId, proxyBid.getUserId(), nextBidAmount, "proxy-" + proxyBid.getId());
            // Directly handle the proxy bid on the aggregate
            Instant proxyServerTs = Instant.now();
            long proxySeqNo = sequenceGenerator.incrementAndGet();
            auctionAgg.handle(proxyCommand, proxyServerTs, proxySeqNo);
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
                PlaceBidCommand autoCommand = new PlaceBidCommand(auctionId, bidderId, decision.getBidAmount(), "automated-" + strategyDecision.getStrategy().getId());

                // Handle the automated bid
                Instant autoServerTs = Instant.now();
                long autoSeqNo = sequenceGenerator.incrementAndGet();
                auctionAgg.handle(autoCommand, autoServerTs, autoSeqNo);
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