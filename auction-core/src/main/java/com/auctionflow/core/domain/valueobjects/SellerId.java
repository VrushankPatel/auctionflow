package com.auctionflow.core.domain.valueobjects;

import java.util.UUID;

public record SellerId(UUID value) {
    public static SellerId of(String value) {
        return new SellerId(UUID.fromString(value));
    }

    public String toString() {
        return value.toString();
    }
}