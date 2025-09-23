package com.auctionflow.api.dtos;

import com.auctionflow.core.domain.valueobjects.AuctionType;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Valid
public class CreateAuctionRequest {
    @NotNull
    private String itemId;
    @NotNull
    private String categoryId;
    @NotNull
    private AuctionType auctionType;
    private BigDecimal reservePrice;
    private BigDecimal buyNowPrice;
    private boolean hiddenReserve;
    @NotNull
    private Instant startTime;
    @NotNull
    private Instant endTime;

    public CreateAuctionRequest() {}

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public AuctionType getAuctionType() { return auctionType; }
    public void setAuctionType(AuctionType auctionType) { this.auctionType = auctionType; }

    public BigDecimal getReservePrice() { return reservePrice; }
    public void setReservePrice(BigDecimal reservePrice) { this.reservePrice = reservePrice; }

    public BigDecimal getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(BigDecimal buyNowPrice) { this.buyNowPrice = buyNowPrice; }

    public boolean isHiddenReserve() { return hiddenReserve; }
    public void setHiddenReserve(boolean hiddenReserve) { this.hiddenReserve = hiddenReserve; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
}