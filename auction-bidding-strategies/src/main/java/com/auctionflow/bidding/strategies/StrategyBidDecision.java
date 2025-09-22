package com.auctionflow.bidding.strategies;

/**
 * Wrapper for bid decision with strategy information
 */
public class StrategyBidDecision {
    private final AutomatedBidStrategy strategy;
    private final BidDecision decision;

    public StrategyBidDecision(AutomatedBidStrategy strategy, BidDecision decision) {
        this.strategy = strategy;
        this.decision = decision;
    }

    public AutomatedBidStrategy getStrategy() { return strategy; }
    public BidDecision getDecision() { return decision; }
}