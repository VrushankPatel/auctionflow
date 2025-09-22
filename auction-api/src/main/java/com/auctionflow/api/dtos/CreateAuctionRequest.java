package com.auctionflow.api.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Valid
public class CreateAuctionRequest {
    @NotNull
    private String itemId;
    @NotNull
    private String categoryId;
    private BigDecimal reservePrice;
    private BigDecimal buyNowPrice;
    @NotNull
    private Instant startTime;
    @NotNull
    private Instant endTime;

    public CreateAuctionRequest() {}

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public BigDecimal getReservePrice() { return reservePrice; }
    public void setReservePrice(BigDecimal reservePrice) { this.reservePrice = reservePrice; }

    public BigDecimal getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(BigDecimal buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
}