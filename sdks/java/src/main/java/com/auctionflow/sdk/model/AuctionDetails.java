package com.auctionflow.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;

public class AuctionDetails {
    private String id;
    private String title;
    private String description;
    private String category;
    private BigDecimal reservePrice;
    private BigDecimal buyNowPrice;
    private BigDecimal currentHighestBid;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private String sellerId;

    public AuctionDetails() {}

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getReservePrice() { return reservePrice; }
    public void setReservePrice(BigDecimal reservePrice) { this.reservePrice = reservePrice; }

    public BigDecimal getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(BigDecimal buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(BigDecimal currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
}