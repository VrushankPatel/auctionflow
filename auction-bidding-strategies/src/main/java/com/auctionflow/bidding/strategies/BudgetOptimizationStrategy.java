package com.auctionflow.bidding.strategies;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Duration;
import java.time.Instant;

/**
 * Strategy that optimizes bidding across multiple auctions within a budget
 */
public class BudgetOptimizationStrategy implements BiddingStrategy {

    @Override
    public BidDecision decideBid(AuctionId auctionId, BidderId bidderId, Money currentHighestBid,
                                Instant auctionEndTime, StrategyParameters params) {
        Money totalBudget = new Money(params.getDouble("totalBudget"));
        Money allocatedBudget = new Money(params.getDouble("allocatedBudget"));
        Integer competingAuctions = params.getInt("competingAuctions");

        Money maxBid = new Money(params.getDouble("maxBid"));
        if (currentHighestBid.isGreaterThanOrEqual(maxBid)) {
            return BidDecision.noBid();
        }

        // Check if we have budget left
        if (allocatedBudget.isGreaterThanOrEqual(totalBudget)) {
            return BidDecision.noBid();
        }

        // Distribute budget across auctions
        Money availablePerAuction = totalBudget.subtract(allocatedBudget).divide(competingAuctions);

        // Only bid if current highest is below our per-auction budget
        if (currentHighestBid.isGreaterThanOrEqual(availablePerAuction)) {
            return BidDecision.noBid();
        }

        // Bid conservatively
        Money nextBid = currentHighestBid.add(new Money(1.0));
        if (nextBid.isGreaterThan(availablePerAuction) || nextBid.isGreaterThan(maxBid)) {
            return BidDecision.noBid();
        }

        return BidDecision.bid(nextBid, Instant.now());
    }

    @Override
    public StrategyType getType() {
        return StrategyType.BUDGET_OPTIMIZATION;
    }
}