package com.auctionflow.analytics.dtos;

public class FraudScoreResponse {
    private double fraudScore;
    private String riskLevel;

    public FraudScoreResponse() {}

    public FraudScoreResponse(double fraudScore, String riskLevel) {
        this.fraudScore = fraudScore;
        this.riskLevel = riskLevel;
    }

    // getters and setters
    public double getFraudScore() { return fraudScore; }
    public void setFraudScore(double fraudScore) { this.fraudScore = fraudScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
}