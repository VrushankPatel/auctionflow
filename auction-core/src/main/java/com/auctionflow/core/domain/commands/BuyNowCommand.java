package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.util.UUID;

public record BuyNowCommand(AuctionId auctionId, UUID buyerId) {
    public BuyNowCommand {
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        if (buyerId == null) {
            throw new IllegalArgumentException("BuyerId cannot be null");
        }
    }
}