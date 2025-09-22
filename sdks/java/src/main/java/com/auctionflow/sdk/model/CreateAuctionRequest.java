package com.auctionflow.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;

public class CreateAuctionRequest {
    private String itemId;
    private String categoryId;
    private String auctionType;
    private BigDecimal reservePrice;
    private BigDecimal buyNowPrice;
    private Instant startTime;
    private Instant endTime;
    private boolean hiddenReserve;

    public CreateAuctionRequest() {}

    public CreateAuctionRequest(String itemId, String categoryId, String auctionType,
                                BigDecimal reservePrice, BigDecimal buyNowPrice,
                                Instant startTime, Instant endTime, boolean hiddenReserve) {
        this.itemId = itemId;
        this.categoryId = categoryId;
        this.auctionType = auctionType;
        this.reservePrice = reservePrice;
        this.buyNowPrice = buyNowPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.hiddenReserve = hiddenReserve;
    }

    // getters and setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getAuctionType() { return auctionType; }
    public void setAuctionType(String auctionType) { this.auctionType = auctionType; }

    public BigDecimal getReservePrice() { return reservePrice; }
    public void setReservePrice(BigDecimal reservePrice) { this.reservePrice = reservePrice; }

    public BigDecimal getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(BigDecimal buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public boolean isHiddenReserve() { return hiddenReserve; }
    public void setHiddenReserve(boolean hiddenReserve) { this.hiddenReserve = hiddenReserve; }
}