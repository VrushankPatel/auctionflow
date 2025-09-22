package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;
import com.auctionflow.core.domain.valueobjects.SellerId;

public record MakeOfferCommand(AuctionId auctionId, BidderId buyerId, SellerId sellerId, Money amount) {
    public MakeOfferCommand {
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId cannot be null");
        }
        if (buyerId == null) {
            throw new IllegalArgumentException("BuyerId cannot be null");
        }
        if (sellerId == null) {
            throw new IllegalArgumentException("SellerId cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
    }
}