package com.auctionflow.core.domain.valueobjects;

public record WinnerId(String value) {
    public WinnerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("WinnerId cannot be null or blank");
        }
    }

    public static WinnerId fromString(String value) {
        return new WinnerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}