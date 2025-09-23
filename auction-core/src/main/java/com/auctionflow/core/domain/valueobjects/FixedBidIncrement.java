package com.auctionflow.core.domain.valueobjects;

import java.math.BigDecimal;

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