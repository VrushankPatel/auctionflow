package com.auctionflow.analytics.dtos;

import java.util.List;

public class PricePredictionRequest {
    private String auctionId;
    private List<Double> historicalPrices;
    private String category;
    private int daysSinceStart;
    private Long sellerId;
    private Double sellerRating;

    public PricePredictionRequest() {}

    public PricePredictionRequest(String auctionId, List<Double> historicalPrices, String category, int daysSinceStart, Long sellerId, Double sellerRating) {
        this.auctionId = auctionId;
        this.historicalPrices = historicalPrices;
        this.category = category;
        this.daysSinceStart = daysSinceStart;
        this.sellerId = sellerId;
        this.sellerRating = sellerRating;
    }

    // getters and setters
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public List<Double> getHistoricalPrices() { return historicalPrices; }
    public void setHistoricalPrices(List<Double> historicalPrices) { this.historicalPrices = historicalPrices; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getDaysSinceStart() { return daysSinceStart; }
    public void setDaysSinceStart(int daysSinceStart) { this.daysSinceStart = daysSinceStart; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public Double getSellerRating() { return sellerRating; }
    public void setSellerRating(Double sellerRating) { this.sellerRating = sellerRating; }
}