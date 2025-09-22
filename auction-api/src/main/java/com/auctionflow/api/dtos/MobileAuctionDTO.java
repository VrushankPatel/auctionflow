package com.auctionflow.api.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public class MobileAuctionDTO {
    private String id;
    private String title;
    private BigDecimal currentBid;
    private Instant endTime;
    private String imageUrl;

    public MobileAuctionDTO() {}

    public MobileAuctionDTO(String id, String title, BigDecimal currentBid, Instant endTime, String imageUrl) {
        this.id = id;
        this.title = title;
        this.currentBid = currentBid;
        this.endTime = endTime;
        this.imageUrl = imageUrl;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getCurrentBid() { return currentBid; }
    public void setCurrentBid(BigDecimal currentBid) { this.currentBid = currentBid; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}