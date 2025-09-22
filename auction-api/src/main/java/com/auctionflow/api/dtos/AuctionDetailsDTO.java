package com.auctionflow.api.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public class AuctionDetailsDTO {
    private String auctionId;
    private String itemId;
    private String sellerId;
    private String title;
    private String description;
    private String status;
    private Instant startTs;
    private Instant endTs;
    private BigDecimal reservePrice;
    private BigDecimal buyNowPrice;
    private boolean hiddenReserve;
    private BigDecimal currentHighestBid;
    private String highestBidderId;
    private Instant lastBidTs;

    public AuctionDetailsDTO(String auctionId, String itemId, String sellerId, String title, String description, String status, Instant startTs, Instant endTs, BigDecimal reservePrice, BigDecimal buyNowPrice, boolean hiddenReserve, BigDecimal currentHighestBid, String highestBidderId, Instant lastBidTs) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.startTs = startTs;
        this.endTs = endTs;
        this.reservePrice = reservePrice;
        this.buyNowPrice = buyNowPrice;
        this.hiddenReserve = hiddenReserve;
        this.currentHighestBid = currentHighestBid;
        this.highestBidderId = highestBidderId;
        this.lastBidTs = lastBidTs;
    }

    // getters and setters
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartTs() { return startTs; }
    public void setStartTs(Instant startTs) { this.startTs = startTs; }

    public Instant getEndTs() { return endTs; }
    public void setEndTs(Instant endTs) { this.endTs = endTs; }

    public BigDecimal getReservePrice() { return reservePrice; }
    public void setReservePrice(BigDecimal reservePrice) { this.reservePrice = reservePrice; }

    public BigDecimal getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(BigDecimal buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public boolean isHiddenReserve() { return hiddenReserve; }
    public void setHiddenReserve(boolean hiddenReserve) { this.hiddenReserve = hiddenReserve; }

    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(BigDecimal currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public String getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }

    public Instant getLastBidTs() { return lastBidTs; }
    public void setLastBidTs(Instant lastBidTs) { this.lastBidTs = lastBidTs; }
}