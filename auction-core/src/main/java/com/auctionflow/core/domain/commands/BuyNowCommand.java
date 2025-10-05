package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;

public record BuyNowCommand(AuctionId auctionId, String buyerId) {
    public BuyNowCommand {
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        if (buyerId == null || buyerId.isBlank()) {
            throw new IllegalArgumentException("BuyerId cannot be null or blank");
        }
    }
}