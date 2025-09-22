package com.auctionflow.bidding.strategies;

import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;

/**
 * Decision result from a bidding strategy
 */
public class BidDecision {
    private final boolean shouldBid;
    private final Money bidAmount;
    private final Instant bidTime;

    public BidDecision(boolean shouldBid, Money bidAmount, Instant bidTime) {
        this.shouldBid = shouldBid;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public static BidDecision noBid() {
        return new BidDecision(false, null, null);
    }

    public static BidDecision bid(Money amount, Instant time) {
        return new BidDecision(true, amount, time);
    }

    public boolean shouldBid() { return shouldBid; }
    public Money getBidAmount() { return bidAmount; }
    public Instant getBidTime() { return bidTime; }
}