package com.auctionflow.analytics.dtos;

import java.time.LocalDateTime;

public class EndTimeRecommendationResponse {
    private LocalDateTime recommendedEndTime;
    private String reason;

    public EndTimeRecommendationResponse() {}

    public EndTimeRecommendationResponse(LocalDateTime recommendedEndTime, String reason) {
        this.recommendedEndTime = recommendedEndTime;
        this.reason = reason;
    }

    // getters and setters
    public LocalDateTime getRecommendedEndTime() { return recommendedEndTime; }
    public void setRecommendedEndTime(LocalDateTime recommendedEndTime) { this.recommendedEndTime = recommendedEndTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}