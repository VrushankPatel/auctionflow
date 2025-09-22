package com.auctionflow.timers;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.events.EventStore;
import com.auctionflow.events.publisher.KafkaEventPublisher;
import io.netty.util.Timeout;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@ManagedResource

/**
 * Service to manage auction close timers.
 * Schedules and reschedules timers for auction closures.
 */
public class AuctionTimerService {

    public static class AuctionSchedule {
        private final AuctionId auctionId;
        private final Instant endTime;

        public AuctionSchedule(AuctionId auctionId, Instant endTime) {
            this.auctionId = auctionId;
            this.endTime = endTime;
        }

        public AuctionId getAuctionId() {
            return auctionId;
        }

        public Instant getEndTime() {
            return endTime;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(AuctionTimerService.class);

    private final HierarchicalTimingWheel timingWheel;
    private final EventStore eventStore;
    private final KafkaEventPublisher eventPublisher;
    private final RedissonClient redissonClient;
    private final DurableScheduler durableScheduler;
    private final ExecutorService schedulingExecutor = new ThreadPoolExecutor(
        5, 20, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100)
    );
    private final Map<AuctionId, Timeout> activeTimers = new ConcurrentHashMap<>();

    public AuctionTimerService(HierarchicalTimingWheel timingWheel, EventStore eventStore,
                               KafkaEventPublisher eventPublisher, RedissonClient redissonClient,
                               DurableScheduler durableScheduler) {
        this.timingWheel = timingWheel;
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
        this.redissonClient = redissonClient;
        this.durableScheduler = durableScheduler;
    }

    /**
     * Schedules a close timer for an auction.
     *
     * @param auctionId the auction ID
     * @param endTime the end time
     */
    public void scheduleAuctionClose(AuctionId auctionId, Instant endTime) {
        schedulingExecutor.submit(() -> {
            try {
                UUID jobId = durableScheduler.scheduleAuctionClose(auctionId, endTime);
                long delay = Math.max(0, endTime.toEpochMilli() - Instant.now().toEpochMilli());
                AuctionCloseTask task = new AuctionCloseTask(auctionId, eventStore, eventPublisher, redissonClient, jobId, durableScheduler);
                Timeout timeout = timingWheel.schedule(task, delay);
                activeTimers.put(auctionId, timeout);
                logger.info("Scheduled close timer for auction {} at {}", auctionId, endTime);
            } catch (Exception e) {
                logger.error("Error scheduling timer for auction {}", auctionId, e);
            }
        });
    }

    /**
     * Schedules close timers for multiple auctions in batch.
     *
     * @param schedules list of auction schedules
     */
    public void scheduleBatch(List<AuctionSchedule> schedules) {
        List<UUID> jobIds = durableScheduler.scheduleAuctionCloses(schedules);
        for (int i = 0; i < schedules.size(); i++) {
            AuctionSchedule schedule = schedules.get(i);
            UUID jobId = jobIds.get(i);
            long delay = Math.max(0, schedule.getEndTime().toEpochMilli() - Instant.now().toEpochMilli());
            AuctionCloseTask task = new AuctionCloseTask(schedule.getAuctionId(), eventStore, eventPublisher, redissonClient, jobId, durableScheduler);
            Timeout timeout = timingWheel.schedule(task, delay);
            activeTimers.put(schedule.getAuctionId(), timeout);
            logger.info("Scheduled close timer for auction {} at {}", schedule.getAuctionId(), schedule.getEndTime());
        }
    }

    /**
     * Reschedules the close timer for an auction to a new end time.
     *
     * @param auctionId the auction ID
     * @param newEndTime the new end time
     */
    public void rescheduleAuctionClose(AuctionId auctionId, Instant newEndTime) {
        // Cancel existing timer
        Timeout existing = activeTimers.get(auctionId);
        if (existing != null && !existing.isCancelled()) {
            existing.cancel();
        }

        // Schedule new timer
        scheduleAuctionClose(auctionId, newEndTime);
    }

    /**
     * Cancels the close timer for an auction.
     *
     * @param auctionId the auction ID
     */
    public void cancelAuctionClose(AuctionId auctionId) {
        Timeout timeout = activeTimers.remove(auctionId);
        if (timeout != null && !timeout.isCancelled()) {
            timeout.cancel();
            logger.info("Cancelled close timer for auction {}", auctionId);
        }
    }

    @ManagedAttribute
    public int getSchedulingQueueDepth() {
        return ((ThreadPoolExecutor) schedulingExecutor).getQueue().size();
    }

    @ManagedAttribute
    public int getActiveTimersCount() {
        return activeTimers.size();
    }
}