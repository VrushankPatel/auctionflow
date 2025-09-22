package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;
import java.util.UUID;

public record PlaceBidCommand(AuctionId auctionId, UUID bidderId, Money amount, String idempotencyKey, Instant serverTs, long seqNo) {
    public PlaceBidCommand {
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        if (bidderId == null) {
            throw new IllegalArgumentException("BidderId cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("IdempotencyKey cannot be null or blank");
        }
        if (serverTs == null) {
            throw new IllegalArgumentException("ServerTs cannot be null");
        }
        if (seqNo <= 0) {
            throw new IllegalArgumentException("SeqNo must be positive");
        }
    }
}