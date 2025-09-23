package com.auctionflow.timers;

import com.auctionflow.core.domain.aggregates.DutchAuctionAggregate;
import com.auctionflow.core.domain.commands.ReducePriceCommand;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.AuctionStatus;
import com.auctionflow.common.service.EventStore;
import com.auctionflow.common.service.EventPublisher;
// import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Task to reduce price in a Dutch auction at scheduled intervals.
 */
public class PriceReductionTask implements TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(PriceReductionTask.class);

    private final AuctionId auctionId;
    private final com.auctionflow.common.service.EventStore eventStore;
    private final com.auctionflow.common.service.EventPublisher eventPublisher;
    private final RedissonClient redissonClient;
    private final DurableScheduler durableScheduler;
    private final TimerMetrics timerMetrics;

    public PriceReductionTask(AuctionId auctionId, com.auctionflow.common.service.EventStore eventStore, EventPublisher eventPublisher, RedissonClient redissonClient, DurableScheduler durableScheduler, TimerMetrics timerMetrics) {
        this.auctionId = auctionId;
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
        this.redissonClient = redissonClient;
        this.durableScheduler = durableScheduler;
        this.timerMetrics = timerMetrics;
    }

    @Override
    // @WithSpan("execute-price-reduction-task")
    public void execute() {
        String lockKey = "auction:" + auctionId.value();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                logger.warn("Could not acquire lock for reducing price in auction {}", auctionId);
                return;
            }

            // Load auction state
            List<DomainEvent> events = eventStore.getEvents(auctionId);
            if (events.isEmpty()) {
                logger.warn("No events found for auction {}", auctionId);
                return;
            }

            DutchAuctionAggregate aggregate = new DutchAuctionAggregate(events);

            // Check if still open
            if (aggregate.getStatus() != AuctionStatus.OPEN) {
                logger.info("Auction {} is not open, status: {}", auctionId, aggregate.getStatus());
                return;
            }

            // Reduce price
            ReducePriceCommand command = new ReducePriceCommand(auctionId);
            aggregate.handle(command);

            List<DomainEvent> newEvents = aggregate.getDomainEvents();
            if (!newEvents.isEmpty()) {
                eventStore.save(newEvents, aggregate.getExpectedVersion());
                for (DomainEvent event : newEvents) {
                    eventPublisher.publish(event);
                }
                logger.info("Reduced price for auction {}", auctionId);
            }

        } catch (Exception e) {
            logger.error("Failed to reduce price for auction {}", auctionId, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}