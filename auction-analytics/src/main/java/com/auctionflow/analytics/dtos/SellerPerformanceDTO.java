package com.auctionflow.analytics.dtos;

import java.math.BigDecimal;

public class SellerPerformanceDTO {

    private Long sellerId;
    private String sellerName;
    private Long totalAuctions;
    private Long successfulAuctions;
    private BigDecimal totalRevenue;
    private BigDecimal averageRating;

    public SellerPerformanceDTO() {}

    public SellerPerformanceDTO(Long sellerId, String sellerName, Long totalAuctions, Long successfulAuctions, BigDecimal totalRevenue, BigDecimal averageRating) {
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.totalAuctions = totalAuctions;
        this.successfulAuctions = successfulAuctions;
        this.totalRevenue = totalRevenue;
        this.averageRating = averageRating;
    }

    // Getters and setters

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public Long getTotalAuctions() {
        return totalAuctions;
    }

    public void setTotalAuctions(Long totalAuctions) {
        this.totalAuctions = totalAuctions;
    }

    public Long getSuccessfulAuctions() {
        return successfulAuctions;
    }

    public void setSuccessfulAuctions(Long successfulAuctions) {
        this.successfulAuctions = successfulAuctions;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }
}