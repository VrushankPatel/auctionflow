package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;

public record CommitBidCommand(
        AuctionId auctionId,
        BidderId bidderId,
        String bidHash,
        String salt
) implements AuctionCommand {
}