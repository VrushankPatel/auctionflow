package com.auctionflow.common.service;

import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.time.Instant;
import java.util.List;

public interface AuctionTimerService {

    class AuctionSchedule {
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

    void scheduleAuctionClose(AuctionId auctionId, Instant endTime);
    void scheduleBatch(List<AuctionSchedule> schedules);
    void rescheduleAuctionClose(AuctionId auctionId, Instant newEndTime);
    void schedulePriceReductions(AuctionId auctionId, long intervalMillis, Instant endTime);
    void cancelAuctionClose(AuctionId auctionId);
    int getActiveTimersCount();
}