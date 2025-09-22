package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.OfferId;

public record RejectOfferCommand(OfferId offerId) {
    public RejectOfferCommand {
        if (offerId == null) {
            throw new IllegalArgumentException("OfferId cannot be null");
        }
    }
}