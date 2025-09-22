package com.auctionflow.core.domain.valueobjects;

import java.util.UUID;

public record BidderId(UUID value) {
    public BidderId {
        if (value == null) {
            throw new IllegalArgumentException("BidderId cannot be null");
        }
    }

    public static BidderId generate() {
        return new BidderId(UUID.randomUUID());
    }

    public static BidderId fromString(String value) {
        return new BidderId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}