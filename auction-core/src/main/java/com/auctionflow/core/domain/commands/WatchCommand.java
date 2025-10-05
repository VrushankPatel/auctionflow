package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;

public record WatchCommand(AuctionId auctionId, String userId) {
    public WatchCommand {
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserId cannot be null or blank");
        }
    }
}