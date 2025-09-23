package com.auctionflow.bidding.strategies;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Strategy that optimizes bid timing to minimize competition and maximize win probability
 */
public class OptimalTimingStrategy implements BiddingStrategy {

    @Override
    public BidDecision decideBid(AuctionId auctionId, BidderId bidderId, Money currentHighestBid,
                                Instant auctionEndTime, StrategyParameters params) {
        Instant now = Instant.now();
        Duration timeLeft = Duration.between(now, auctionEndTime);

        Money maxBid = Money.usd(BigDecimal.valueOf(params.getDouble("maxBid")));
        if (currentHighestBid.isGreaterThanOrEqual(maxBid)) {
            return BidDecision.noBid();
        }

        // Optimal timing: bid when activity is low, avoid peak times
        // For simplicity, bid in the middle third of the auction
        Duration totalDuration = Duration.between(Instant.now().minus(timeLeft), auctionEndTime);
        Duration optimalStart = totalDuration.dividedBy(3);
        Duration optimalEnd = totalDuration.multipliedBy(2).dividedBy(3);

        if (timeLeft.compareTo(optimalEnd) > 0 || timeLeft.compareTo(optimalStart) < 0) {
            return BidDecision.noBid();
        }

        // Calculate bid amount - jump to discourage others
        Money increment = Money.usd(BigDecimal.valueOf(params.getDouble("increment")));
        Money nextBid = currentHighestBid.add(increment.multiply(BigDecimal.valueOf(2))); // Bid higher to discourage
        if (nextBid.isGreaterThan(maxBid)) {
            nextBid = maxBid;
        }

        return BidDecision.bid(nextBid, now);
    }

    @Override
    public StrategyType getType() {
        return StrategyType.OPTIMAL_TIMING;
    }
}