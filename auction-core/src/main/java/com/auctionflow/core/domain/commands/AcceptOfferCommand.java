package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.OfferId;

public record AcceptOfferCommand(OfferId offerId) {
    public AcceptOfferCommand {
        if (offerId == null) {
            throw new IllegalArgumentException("OfferId cannot be null");
        }
    }
}