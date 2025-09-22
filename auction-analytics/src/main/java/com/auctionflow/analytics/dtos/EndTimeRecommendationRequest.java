package com.auctionflow.analytics.dtos;

import java.time.LocalDateTime;

public class EndTimeRecommendationRequest {
    private String auctionId;
    private LocalDateTime currentEndTime;
    private int bidCount;
    private double currentPrice;
    private String category;

    public EndTimeRecommendationRequest() {}

    public EndTimeRecommendationRequest(String auctionId, LocalDateTime currentEndTime, int bidCount, double currentPrice, String category) {
        this.auctionId = auctionId;
        this.currentEndTime = currentEndTime;
        this.bidCount = bidCount;
        this.currentPrice = currentPrice;
        this.category = category;
    }

    // getters and setters
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public LocalDateTime getCurrentEndTime() { return currentEndTime; }
    public void setCurrentEndTime(LocalDateTime currentEndTime) { this.currentEndTime = currentEndTime; }

    public int getBidCount() { return bidCount; }
    public void setBidCount(int bidCount) { this.bidCount = bidCount; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}