package com.auctionflow.timers;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Metrics for tracking timer tasks.
 */
@Component
public class TimerMetrics {

    private final Counter scheduled;
    private final Counter executed;
    private final Counter cancelled;
    private final Timer auctionCloseLatency;
    private final DistributionSummary timerAccuracy;

    public TimerMetrics(MeterRegistry meterRegistry) {
        this.scheduled = Counter.builder("timer_tasks_scheduled_total")
                .description("Total number of timer tasks scheduled")
                .register(meterRegistry);
        this.executed = Counter.builder("timer_tasks_executed_total")
                .description("Total number of timer tasks executed")
                .register(meterRegistry);
        this.cancelled = Counter.builder("timer_tasks_cancelled_total")
                .description("Total number of timer tasks cancelled")
                .register(meterRegistry);
        this.auctionCloseLatency = Timer.builder("auction_close_duration")
                .description("Time taken to close an auction")
                .register(meterRegistry);
        this.timerAccuracy = DistributionSummary.builder("timer_accuracy_delay")
                .description("Delay in timer firing (milliseconds)")
                .register(meterRegistry);
    }

    public void incrementScheduled() {
        scheduled.increment();
    }

    public void incrementExecuted() {
        executed.increment();
    }

    public void incrementCancelled() {
        cancelled.increment();
    }

    public Timer.Sample startAuctionCloseTimer() {
        return Timer.start();
    }

    public void recordAuctionCloseLatency(Timer.Sample sample) {
        sample.stop(auctionCloseLatency);
    }

    public void recordTimerAccuracy(long delayMs) {
        timerAccuracy.record(delayMs);
    }
}