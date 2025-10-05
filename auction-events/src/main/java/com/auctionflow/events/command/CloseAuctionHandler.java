package com.auctionflow.events.command;

import com.auctionflow.common.exceptions.OptimisticLockException;
import com.auctionflow.core.domain.aggregates.AggregateRoot;
import com.auctionflow.core.domain.aggregates.AuctionAggregate;
import com.auctionflow.core.domain.aggregates.DutchAuctionAggregate;
import com.auctionflow.core.domain.commands.CloseAuctionCommand;
import com.auctionflow.core.domain.events.AuctionCreatedEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.AuctionType;
import com.auctionflow.common.service.EventStore;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class CloseAuctionHandler {

    private final EventStore eventStore;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final RedissonClient redissonClient;

    public CloseAuctionHandler(EventStore eventStore, KafkaTemplate<String, DomainEvent> kafkaTemplate, RedissonClient redissonClient) {
        this.eventStore = eventStore;
        this.kafkaTemplate = kafkaTemplate;
        this.redissonClient = redissonClient;
    }

    @Async
    @EventListener
    public void handle(CloseAuctionCommand command) {
        String lockKey = "auction:" + command.auctionId().value();
        RLock lock = redissonClient.getLock(lockKey);
        try {
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
                        .orElse(AuctionType.ENGLISH_OPEN);
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
                    aggregate.clearDomainEvents();
                    break;
                } catch (OptimisticLockException e) {
                    if (attempt == maxRetries) {
                        throw e;
                    }
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
}