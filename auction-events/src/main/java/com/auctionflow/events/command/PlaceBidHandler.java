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

@Component
public class PlaceBidHandler implements CommandHandler<PlaceBidCommand> {

    private final EventStore eventStore;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final RedissonClient redissonClient;
    private final AntiSnipeExtension antiSnipeExtension;
    private final ProxyBidRepository proxyBidRepository;

    public PlaceBidHandler(EventStore eventStore, KafkaTemplate<String, DomainEvent> kafkaTemplate, RedissonClient redissonClient, AntiSnipeExtension antiSnipeExtension, ProxyBidRepository proxyBidRepository) {
        this.eventStore = eventStore;
        this.kafkaTemplate = kafkaTemplate;
        this.redissonClient = redissonClient;
        this.antiSnipeExtension = antiSnipeExtension;
        this.proxyBidRepository = proxyBidRepository;
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
                    List<DomainEvent> events = eventStore.getEvents(command.auctionId());
                    AuctionType type = events.stream()
                        .filter(e -> e instanceof AuctionCreatedEvent)
                        .map(e -> ((AuctionCreatedEvent) e).getAuctionType())
                        .findFirst()
                        .orElse(AuctionType.ENGLISH_OPEN); // default for old auctions
                    AggregateRoot aggregate;
                    if (type == AuctionType.DUTCH) {
                        DutchAuctionAggregate dutch = new DutchAuctionAggregate(events);
                        dutch.handle(command);
                        aggregate = dutch;
                    } else {
                        AuctionAggregate english = new AuctionAggregate(events);
                        english.handle(command);
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
        Optional<com.auctionflow.core.domain.valueobjects.Bid> highestBidOpt = auctionAgg.getBids().stream()
            .max(java.util.Comparator.comparing(com.auctionflow.core.domain.valueobjects.Bid::amount));

        if (highestBidOpt.isEmpty()) {
            return;
        }

        Money currentHighest = highestBidOpt.get().amount();

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
}