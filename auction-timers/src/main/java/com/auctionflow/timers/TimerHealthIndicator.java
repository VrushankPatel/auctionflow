package com.auctionflow.timers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class TimerHealthIndicator implements HealthIndicator {

    @Autowired
    private AuctionTimerService auctionTimerService;

    @Autowired
    private HierarchicalTimingWheel timingWheel;

    @Override
    public Health health() {
        try {
            // Check if timing wheel is operational
            if (timingWheel != null) {
                int activeTimers = auctionTimerService.getActiveTimersCount();
                return Health.up()
                    .withDetail("timerService", "Operational")
                    .withDetail("activeTimers", activeTimers)
                    .build();
            } else {
                return Health.down().withDetail("timerService", "Timing wheel not initialized").build();
            }
        } catch (Exception e) {
            return Health.down(e).withDetail("timerService", "Error checking timer status").build();
        }
    }
}