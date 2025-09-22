package com.auctionflow.core.domain.valueobjects;

import java.util.UUID;

public record WinnerId(UUID value) {
    public WinnerId {
        if (value == null) {
            throw new IllegalArgumentException("WinnerId cannot be null");
        }
    }

    public static WinnerId fromString(String value) {
        return new WinnerId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}