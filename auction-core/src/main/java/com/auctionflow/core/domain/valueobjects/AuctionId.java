package com.auctionflow.core.domain.valueobjects;

import java.util.UUID;

public record AuctionId(String value) {
    public AuctionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AuctionId cannot be null or blank");
        }
    }

    public static AuctionId generate() {
        return new AuctionId(UUID.randomUUID().toString());
    }

    public static AuctionId fromString(String value) {
        return new AuctionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}