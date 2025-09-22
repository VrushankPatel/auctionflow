package com.auctionflow.core.domain.valueobjects;

import java.util.UUID;

public record OfferId(UUID value) {
    public static OfferId generate() {
        return new OfferId(UUID.randomUUID());
    }

    public String toString() {
        return value.toString();
    }
}