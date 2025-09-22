package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.time.Instant;

/**
 * Command to replay and rebuild an aggregate from events.
 */
public record ReplayAggregateCommand(AuctionId aggregateId, Instant fromTimestamp) {
    public ReplayAggregateCommand {
        if (aggregateId == null) {
            throw new IllegalArgumentException("AggregateId cannot be null");
        }
    }

    public ReplayAggregateCommand(AuctionId aggregateId) {
        this(aggregateId, null);
    }
}