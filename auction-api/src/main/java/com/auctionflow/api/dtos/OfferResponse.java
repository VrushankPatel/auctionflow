package com.auctionflow.api.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public class OfferResponse {
    private String offerId;
    private String auctionId;
    private String buyerId;
    private String sellerId;
    private BigDecimal amount;
    private String status;
    private Instant createdAt;

    // Constructor
    public OfferResponse(String offerId, String auctionId, String buyerId, String sellerId, BigDecimal amount, String status, Instant createdAt) {
        this.offerId = offerId;
        this.auctionId = auctionId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}