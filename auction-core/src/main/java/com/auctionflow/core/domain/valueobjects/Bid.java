package com.auctionflow.core.domain.valueobjects;

import java.time.Instant;

public record Bid(BidderId bidderId, Money amount, Instant timestamp) {
    public Bid {
        if (bidderId == null) {
            throw new IllegalArgumentException("BidderId cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }
}