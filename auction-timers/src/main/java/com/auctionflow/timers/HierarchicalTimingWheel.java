package com.auctionflow.timers;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * HierarchicalTimingWheel implementation using Netty's HashedWheelTimer.
 * Configured with 100ms tick duration and 8192 wheel size.
 */
@Component
@ManagedResource
public class HierarchicalTimingWheel {

    private static final Logger logger = LoggerFactory.getLogger(HierarchicalTimingWheel.class);

    private final HashedWheelTimer wheelTimer;
    private final TimerMetrics metrics;
    private final ThreadPoolExecutor executionExecutor = new ThreadPoolExecutor(
        10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1000)
    );

    public HierarchicalTimingWheel(TimerMetrics metrics) {
        this.metrics = metrics;
        // 100ms tick duration, 8192 ticks per wheel
        this.wheelTimer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 8192);
    }

    /**
     * Schedules a task to run after the specified delay.
     *
     * @param task  the task to execute
     * @param delay the delay in milliseconds
     * @return Timeout handle for cancellation
     */
    public Timeout schedule(TimerTask task, long delay) {
        metrics.incrementScheduled();
        return wheelTimer.newTimeout(new NettyTimerTaskAdapter(task, metrics), delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the timing wheel.
     */
    public void stop() {
        wheelTimer.stop();
        executionExecutor.shutdown();
    }

    @ManagedAttribute
    public int getExecutionQueueDepth() {
        return executionExecutor.getQueue().size();
    }

    /**
     * Adapter to bridge our TimerTask to Netty's TimerTask.
     */
    private static class NettyTimerTaskAdapter implements TimerTask {

        private final com.auctionflow.timers.TimerTask task;
        private final TimerMetrics metrics;

        public NettyTimerTaskAdapter(com.auctionflow.timers.TimerTask task, TimerMetrics metrics) {
            this.task = task;
            this.metrics = metrics;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            if (!timeout.isCancelled()) {
                executionExecutor.submit(() -> {
                    try {
                        task.execute();
                        metrics.incrementExecuted();
                    } catch (Exception e) {
                        logger.error("Error executing timer task", e);
                    }
                });
            } else {
                metrics.incrementCancelled();
            }
        }
    }
}