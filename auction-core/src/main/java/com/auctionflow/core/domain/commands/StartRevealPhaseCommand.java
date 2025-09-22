package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;

public record StartRevealPhaseCommand(
        AuctionId auctionId
) implements AuctionCommand {
}