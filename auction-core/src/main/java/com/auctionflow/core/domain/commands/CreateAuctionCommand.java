package com.auctionflow.core.domain.commands;

import com.auctionflow.core.domain.valueobjects.AntiSnipePolicy;
import com.auctionflow.core.domain.valueobjects.AuctionType;
import com.auctionflow.core.domain.valueobjects.ItemId;
import com.auctionflow.core.domain.valueobjects.Money;
import com.auctionflow.core.domain.valueobjects.SellerId;

import java.time.Instant;

public record CreateAuctionCommand(ItemId itemId, SellerId sellerId, String categoryId, AuctionType auctionType, Money reservePrice, Money buyNowPrice, Instant startTime, Instant endTime, AntiSnipePolicy antiSnipePolicy, boolean hiddenReserve) {
    public CreateAuctionCommand {
        if (itemId == null) {
            throw new IllegalArgumentException("ItemId cannot be null");
        }
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("CategoryId cannot be null or blank");
        }
        if (auctionType == null) {
            throw new IllegalArgumentException("AuctionType cannot be null");
        }
        if (reservePrice == null) {
            throw new IllegalArgumentException("ReservePrice cannot be null");
        }
        if (buyNowPrice == null) {
            throw new IllegalArgumentException("BuyNowPrice cannot be null");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("StartTime cannot be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("EndTime cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("StartTime must be before EndTime");
        }
        if (antiSnipePolicy == null) {
            throw new IllegalArgumentException("AntiSnipePolicy cannot be null");
        }
    }
}