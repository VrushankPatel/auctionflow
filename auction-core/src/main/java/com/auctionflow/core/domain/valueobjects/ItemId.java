package com.auctionflow.core.domain.valueobjects;

import java.util.UUID;

public record ItemId(String value) {
    public ItemId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ItemId cannot be null or blank");
        }
    }

    public static ItemId generate() {
        return new ItemId(UUID.randomUUID().toString());
    }

    public static ItemId fromString(String value) {
        return new ItemId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}