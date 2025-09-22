package com.auctionflow.core.domain.valueobjects;

import java.util.UUID;

public record BidderId(UUID id) {
    public BidderId {
        if (id == null) {
            throw new IllegalArgumentException("BidderId cannot be null");
        }
    }

    public static BidderId generate() {
        return new BidderId(UUID.randomUUID());
    }

    public static BidderId fromString(String id) {
        return new BidderId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return id.toString();
    }
}