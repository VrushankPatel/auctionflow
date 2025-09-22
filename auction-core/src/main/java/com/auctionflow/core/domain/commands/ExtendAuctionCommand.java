package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.time.Instant;

public record ExtendAuctionCommand(AuctionId auctionId, Instant newEndTime) {
    public ExtendAuctionCommand {
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        if (newEndTime == null) {
            throw new IllegalArgumentException("NewEndTime cannot be null");
        }
    }
}