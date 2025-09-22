package com.auctionflow.core.domain.valueobjects;

import java.time.Duration;

public record DutchAuctionRules(Money minimumPrice, Money decrementAmount, Duration decrementInterval) {
    public DutchAuctionRules {
        if (minimumPrice == null) {
            throw new IllegalArgumentException("MinimumPrice cannot be null");
        }
        if (decrementAmount == null) {
            throw new IllegalArgumentException("DecrementAmount cannot be null");
        }
        if (decrementInterval == null) {
            throw new IllegalArgumentException("DecrementInterval cannot be null");
        }
        if (decrementAmount.isLessThanOrEqual(Money.ZERO)) {
            throw new IllegalArgumentException("DecrementAmount must be positive");
        }
        if (decrementInterval.isNegative() || decrementInterval.isZero()) {
            throw new IllegalArgumentException("DecrementInterval must be positive");
        }
    }
}