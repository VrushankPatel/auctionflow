package com.auctionflow.core.domain.valueobjects;

import java.util.UUID;

public record AuctionId(UUID value) {
    public AuctionId {
        if (value == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
    }

    public static AuctionId generate() {
        return new AuctionId(UUID.randomUUID());
    }

    public static AuctionId fromString(String value) {
        return new AuctionId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}