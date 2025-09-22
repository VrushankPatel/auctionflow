package com.auctionflow.timers;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for tracking timer tasks.
 */
@Component
@ManagedResource
public class TimerMetrics {

    private final AtomicLong scheduled = new AtomicLong(0);
    private final AtomicLong executed = new AtomicLong(0);
    private final AtomicLong cancelled = new AtomicLong(0);

    public void incrementScheduled() {
        scheduled.incrementAndGet();
    }

    public void incrementExecuted() {
        executed.incrementAndGet();
    }

    public void incrementCancelled() {
        cancelled.incrementAndGet();
    }

    @ManagedAttribute
    public long getScheduled() {
        return scheduled.get();
    }

    @ManagedAttribute
    public long getExecuted() {
        return executed.get();
    }

    @ManagedAttribute
    public long getCancelled() {
        return cancelled.get();
    }
}