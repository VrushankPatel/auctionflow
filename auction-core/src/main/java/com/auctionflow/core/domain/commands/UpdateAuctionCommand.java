package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;

public class UpdateAuctionCommand {
    private final AuctionId auctionId;
    private final String title;
    private final String description;

    public UpdateAuctionCommand(AuctionId auctionId, String title, String description) {
        this.auctionId = auctionId;
        this.title = title;
        this.description = description;
    }

    public AuctionId getAuctionId() { return auctionId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}