package com.auctionflow.analytics.dtos;

import java.util.List;

public class RecommendationResponse {
    private List<Long> recommendedAuctionIds;

    // Getters and setters
    public List<Long> getRecommendedAuctionIds() {
        return recommendedAuctionIds;
    }

    public void setRecommendedAuctionIds(List<Long> recommendedAuctionIds) {
        this.recommendedAuctionIds = recommendedAuctionIds;
    }
}