package com.auctionflow.bidding.strategies;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Duration;
import java.time.Instant;

/**
 * Strategy to prevent sniping by bidding early and incrementally
 */
public class SnipingPreventionStrategy implements BiddingStrategy {

    @Override
    public BidDecision decideBid(AuctionId auctionId, BidderId bidderId, Money currentHighestBid,
                                Instant auctionEndTime, StrategyParameters params) {
        Instant now = Instant.now();
        Duration timeLeft = Duration.between(now, auctionEndTime);

        // Don't bid in the last minute to avoid triggering anti-snipe
        if (timeLeft.toMinutes() < 1) {
            return BidDecision.noBid();
        }

        // Bid incrementally, not too aggressively
        Money maxBid = new Money(params.getDouble("maxBid"));
        if (currentHighestBid.isGreaterThanOrEqual(maxBid)) {
            return BidDecision.noBid();
        }

        // Calculate next bid amount - conservative increment
        Money increment = new Money(params.getDouble("increment"));
        Money nextBid = currentHighestBid.add(increment);
        if (nextBid.isGreaterThan(maxBid)) {
            nextBid = maxBid;
        }

        // Bid immediately if we're not the current high bidder
        return BidDecision.bid(nextBid, now);
    }

    @Override
    public StrategyType getType() {
        return StrategyType.SNIPING_PREVENTION;
    }
}