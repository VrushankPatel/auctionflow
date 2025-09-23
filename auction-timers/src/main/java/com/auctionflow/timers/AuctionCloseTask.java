package com.auctionflow.timers;

import com.auctionflow.core.domain.aggregates.AggregateRoot;
import com.auctionflow.core.domain.aggregates.AuctionAggregate;
import com.auctionflow.core.domain.aggregates.DutchAuctionAggregate;
import com.auctionflow.core.domain.commands.CloseAuctionCommand;
import com.auctionflow.core.domain.commands.StartRevealPhaseCommand;
import com.auctionflow.core.domain.events.AuctionCreatedEvent;
import com.auctionflow.core.domain.events.AuctionRevealPhaseStartedEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.AuctionStatus;
import com.auctionflow.core.domain.valueobjects.AuctionType;
import com.auctionflow.common.service.EventStore;
import com.auctionflow.common.service.EventPublisher;
import io.micrometer.core.instrument.Timer;
// import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Task to close an auction when its end time is reached.
 * Loads auction state, checks if still open, determines winner, emits AuctionClosedEvent.
 * Idempotent with database state check and distributed lock to prevent duplicate execution.
 */
public class AuctionCloseTask implements TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(AuctionCloseTask.class);

    private final AuctionId auctionId;
    private final com.auctionflow.common.service.EventStore eventStore;
    private final EventPublisher eventPublisher;
    private final RedissonClient redissonClient;
    private final UUID jobId;
    private final DurableScheduler durableScheduler;
    private final TimerMetrics timerMetrics;

    public AuctionCloseTask(AuctionId auctionId, com.auctionflow.common.service.EventStore eventStore, EventPublisher eventPublisher, RedissonClient redissonClient, UUID jobId, DurableScheduler durableScheduler, TimerMetrics timerMetrics) {
        this.auctionId = auctionId;
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
        this.redissonClient = redissonClient;
        this.jobId = jobId;
        this.durableScheduler = durableScheduler;
        this.timerMetrics = timerMetrics;
    }

    @Override
    // @WithSpan("execute-auction-close-task")
    public void execute() {
        Timer.Sample sample = timerMetrics.startAuctionCloseTimer();
        String lockKey = "auction-close:" + auctionId.value();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                logger.warn("Could not acquire lock for closing auction {}", auctionId);
                return;
            }

            // Load auction state from event store
            List<DomainEvent> events = eventStore.getEvents(auctionId);
            if (events.isEmpty()) {
                logger.warn("No events found for auction {}", auctionId);
                return;
            }

            AuctionType type = events.stream()
                .filter(e -> e instanceof AuctionCreatedEvent)
                .map(e -> ((AuctionCreatedEvent) e).getAuctionType())
                .findFirst()
                .orElse(AuctionType.ENGLISH_OPEN);
            AggregateRoot aggregate;
            if (type == AuctionType.DUTCH) {
                aggregate = new DutchAuctionAggregate(events);
            } else {
                aggregate = new AuctionAggregate(events);
            }

            // Check if auction is still open or in phases
            if (aggregate.getStatus() != AuctionStatus.OPEN &&
                aggregate.getStatus() != AuctionStatus.SEALED_BIDDING &&
                aggregate.getStatus() != AuctionStatus.REVEAL_PHASE) {
                logger.info("Auction {} is already closed or not active, status: {}", auctionId, aggregate.getStatus());
                return;
            }

            // Check if end time has passed
            Instant now = Instant.now();
            if (now.isBefore(aggregate.getEndTime())) {
                logger.info("Auction {} end time not reached yet, endTime: {}, now: {}", auctionId, aggregate.getEndTime(), now);
                return;
            }
            // Record timer accuracy: delay in milliseconds
            long delayMs = now.toEpochMilli() - aggregate.getEndTime().toEpochMilli();
            timerMetrics.recordTimerAccuracy(delayMs);

            // Handle based on type and status
            if (type == AuctionType.SEALED_BID && aggregate.getStatus() == AuctionStatus.SEALED_BIDDING) {
                StartRevealPhaseCommand command = new StartRevealPhaseCommand(auctionId);
                aggregate.handle(command);
            } else {
                CloseAuctionCommand command = new CloseAuctionCommand(auctionId);
                aggregate.handle(command);
            }

            List<DomainEvent> newEvents = aggregate.getDomainEvents();
            if (!newEvents.isEmpty()) {
                // Save events to event store
                eventStore.save(newEvents, aggregate.getExpectedVersion());

                // Publish events
                for (DomainEvent event : newEvents) {
                    eventPublisher.publish(event);
                    // If reveal phase started, schedule close for reveal end
                    if (event instanceof AuctionRevealPhaseStartedEvent revealEvent) {
                        durableScheduler.scheduleAuctionClose(auctionId, revealEvent.getRevealEndTime());
                    }
                }

                if (aggregate.getStatus() == AuctionStatus.CLOSED) {
                    logger.info("Successfully closed auction {} with winner {}", auctionId, aggregate.getWinnerId());
                } else {
                    logger.info("Started reveal phase for auction {}", auctionId);
                }
                durableScheduler.markJobCompleted(jobId);
            } else {
                logger.warn("No events generated for closing auction {}", auctionId);
                durableScheduler.handleJobFailure(jobId);
            }

        } catch (Exception e) {
            logger.error("Failed to close auction {}", auctionId, e);
            durableScheduler.handleJobFailure(jobId);
            // In a real system, might want to retry or send to DLQ
        } finally {
            timerMetrics.recordAuctionCloseLatency(sample);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}