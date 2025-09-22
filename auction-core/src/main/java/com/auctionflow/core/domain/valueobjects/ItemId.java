package com.auctionflow.core.domain.valueobjects;

import java.util.UUID;

public record ItemId(UUID value) {
    public ItemId {
        if (value == null) {
            throw new IllegalArgumentException("ItemId cannot be null");
        }
    }

    public static ItemId generate() {
        return new ItemId(UUID.randomUUID());
    }

    public static ItemId fromString(String value) {
        return new ItemId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}