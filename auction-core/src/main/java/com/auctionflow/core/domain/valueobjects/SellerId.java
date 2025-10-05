package com.auctionflow.core.domain.valueobjects;

import java.util.UUID;

public record SellerId(String value) {
    public SellerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SellerId cannot be null or blank");
        }
    }

    public static SellerId generate() {
        return new SellerId(UUID.randomUUID().toString());
    }

    public static SellerId of(String value) {
        return new SellerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}