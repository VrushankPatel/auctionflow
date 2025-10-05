package com.auctionflow.core.domain.valueobjects;

import java.util.UUID;

public record BidderId(String id) {
    public BidderId {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("BidderId cannot be null or blank");
        }
    }

    public static BidderId generate() {
        return new BidderId(UUID.randomUUID().toString());
    }

    public static BidderId fromString(String id) {
        return new BidderId(id);
    }

    @Override
    public String toString() {
        return id;
    }
}