package com.auctionflow.analytics.dtos;

public class PricePredictionResponse {
    private double predictedPrice;
    private double confidence;

    public PricePredictionResponse() {}

    public PricePredictionResponse(double predictedPrice, double confidence) {
        this.predictedPrice = predictedPrice;
        this.confidence = confidence;
    }

    // getters and setters
    public double getPredictedPrice() { return predictedPrice; }
    public void setPredictedPrice(double predictedPrice) { this.predictedPrice = predictedPrice; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}