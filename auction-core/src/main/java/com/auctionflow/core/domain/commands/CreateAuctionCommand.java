package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.ItemId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;

public record CreateAuctionCommand(ItemId itemId, Money reservePrice, Money buyNowPrice, Instant startTime, Instant endTime) {
    public CreateAuctionCommand {
        if (itemId == null) {
            throw new IllegalArgumentException("ItemId cannot be null");
        }
        if (reservePrice == null) {
            throw new IllegalArgumentException("ReservePrice cannot be null");
        }
        if (buyNowPrice == null) {
            throw new IllegalArgumentException("BuyNowPrice cannot be null");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("StartTime cannot be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("EndTime cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("StartTime must be before EndTime");
        }
    }
}