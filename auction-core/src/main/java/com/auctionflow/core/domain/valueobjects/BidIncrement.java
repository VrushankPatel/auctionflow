package com.auctionflow.core.domain.valueobjects;

import java.math.BigDecimal;

public interface BidIncrement {
    Money nextBid(Money currentHighest);
}

public class FixedBidIncrement implements BidIncrement {
    private final Money increment;

    public FixedBidIncrement(Money increment) {
        this.increment = increment;
    }

    @Override
    public Money nextBid(Money currentHighest) {
        return currentHighest.add(increment);
    }
}

class PercentageBidIncrement implements BidIncrement {
    private final BigDecimal percentage;

    public PercentageBidIncrement(BigDecimal percentage) {
        this.percentage = percentage;
    }

    @Override
    public Money nextBid(Money currentHighest) {
        BigDecimal incrementAmount = currentHighest.amount().multiply(percentage);
        Money increment = new Money(incrementAmount, currentHighest.currency());
        return currentHighest.add(increment);
    }
}

class DynamicBidIncrement implements BidIncrement {
    // For simplicity, assume a ladder, but implement as fixed for now
    private final Money increment;

    public DynamicBidIncrement(Money increment) {
        this.increment = increment;
    }

    @Override
    public Money nextBid(Money currentHighest) {
        return currentHighest.add(increment);
    }
}