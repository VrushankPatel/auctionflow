package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;

public record ReducePriceCommand(AuctionId auctionId) {
    public ReducePriceCommand {
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
    }
}