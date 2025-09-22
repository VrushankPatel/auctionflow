package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;

public record RevealBidCommand(
        AuctionId auctionId,
        BidderId bidderId,
        Money amount,
        String salt
) implements AuctionCommand {
}