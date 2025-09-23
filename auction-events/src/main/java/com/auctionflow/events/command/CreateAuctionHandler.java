package com.auctionflow.events.command;

import com.auctionflow.core.domain.aggregates.AggregateRoot;
import com.auctionflow.core.domain.aggregates.AuctionAggregate;
import com.auctionflow.core.domain.aggregates.DutchAuctionAggregate;
import com.auctionflow.core.domain.commands.CreateAuctionCommand;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.AuctionId;
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
public class CreateAuctionHandler implements CommandHandler<CreateAuctionCommand> {

    private final EventStore eventStore;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final RedissonClient redissonClient;

    public CreateAuctionHandler(EventStore eventStore, KafkaTemplate<String, DomainEvent> kafkaTemplate, RedissonClient redissonClient) {
        this.eventStore = eventStore;
        this.kafkaTemplate = kafkaTemplate;
        this.redissonClient = redissonClient;
    }

    @Override
    @Async
    @EventListener
    public void handle(CreateAuctionCommand command) {
        AggregateRoot aggregate;
        if (command.auctionType() == AuctionType.DUTCH) {
            aggregate = new DutchAuctionAggregate();
        } else {
            aggregate = new AuctionAggregate();
        }
        aggregate.handle(command);
        List<DomainEvent> events = aggregate.getDomainEvents();
        String lockKey = "auction:" + ((AuctionId) aggregate.getId()).value();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Could not acquire lock for auction " + aggregate.getId());
            }
            eventStore.save(events, aggregate.getExpectedVersion());
            // Publish to Kafka
            for (DomainEvent event : events) {
                kafkaTemplate.send("auction-events", event.getAggregateId().toString(), event);
            }
            aggregate.clearDomainEvents();
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