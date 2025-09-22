package com.auctionflow.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;

public class AuctionSummary {
    private String id;
    private String title;
    private String category;
    private BigDecimal currentBid;
    private Instant endTime;
    private String status;

    public AuctionSummary() {}

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getCurrentBid() { return currentBid; }
    public void setCurrentBid(BigDecimal currentBid) { this.currentBid = currentBid; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}