package com.auctionflow.timers;

import com.auctionflow.common.service.EventPublisher;
import com.auctionflow.common.service.EventStore;
import com.auctionflow.core.domain.commands.ReducePriceCommand;
import com.auctionflow.core.domain.valueobjects.AuctionId;
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
import java.util.UUID;
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
public class AuctionTimerService implements com.auctionflow.common.service.AuctionTimerService {



    private static final Logger logger = LoggerFactory.getLogger(AuctionTimerService.class);

    private final HierarchicalTimingWheel timingWheel;
    private final EventStore eventStore;
    private final EventPublisher eventPublisher;
    private final RedissonClient redissonClient;
    private final DurableScheduler durableScheduler;
    private final TimerMetrics timerMetrics;
    private final ExecutorService schedulingExecutor = new ThreadPoolExecutor(
        5, 20, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100)
    );
    private final Map<AuctionId, Timeout> activeTimers = new ConcurrentHashMap<>();

    public AuctionTimerService(HierarchicalTimingWheel timingWheel, EventStore eventStore,
                                EventPublisher eventPublisher, RedissonClient redissonClient,
                                DurableScheduler durableScheduler, TimerMetrics timerMetrics) {
        this.timingWheel = timingWheel;
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
        this.redissonClient = redissonClient;
        this.durableScheduler = durableScheduler;
        this.timerMetrics = timerMetrics;
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
                AuctionCloseTask task = new AuctionCloseTask(auctionId, eventStore, eventPublisher, redissonClient, jobId, durableScheduler, timerMetrics);
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
            AuctionCloseTask task = new AuctionCloseTask(schedule.getAuctionId(), eventStore, eventPublisher, redissonClient, jobId, durableScheduler, timerMetrics);
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
     * Schedules periodic price reductions for a Dutch auction.
     *
     * @param auctionId the auction ID
     * @param intervalMillis the reduction interval in milliseconds
     * @param endTime the auction end time
     */
    public void schedulePriceReductions(AuctionId auctionId, long intervalMillis, Instant endTime) {
        schedulingExecutor.submit(() -> {
            try {
                Instant now = Instant.now();
                Instant nextReduction = now.plusMillis(intervalMillis);
                while (nextReduction.isBefore(endTime)) {
                    long delay = nextReduction.toEpochMilli() - now.toEpochMilli();
                    if (delay > 0) {
                        PriceReductionTask task = new PriceReductionTask(auctionId, eventStore, eventPublisher, redissonClient, durableScheduler, timerMetrics);
                        Timeout timeout = timingWheel.schedule(task, delay);
                        // For simplicity, not tracking all reductions, but could add to activeTimers with key
                        logger.info("Scheduled price reduction for auction {} at {}", auctionId, nextReduction);
                    }
                    nextReduction = nextReduction.plusMillis(intervalMillis);
                }
            } catch (Exception e) {
                logger.error("Error scheduling price reductions for auction {}", auctionId, e);
            }
        });
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