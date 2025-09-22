package com.auctionflow.analytics.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AuctionSummaryDTO {

    private LocalDate date;
    private Long totalAuctions;
    private Long totalBids;
    private BigDecimal totalRevenue;

    public AuctionSummaryDTO() {}

    public AuctionSummaryDTO(LocalDate date, Long totalAuctions, Long totalBids, BigDecimal totalRevenue) {
        this.date = date;
        this.totalAuctions = totalAuctions;
        this.totalBids = totalBids;
        this.totalRevenue = totalRevenue;
    }

    // Getters and setters

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getTotalAuctions() {
        return totalAuctions;
    }

    public void setTotalAuctions(Long totalAuctions) {
        this.totalAuctions = totalAuctions;
    }

    public Long getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(Long totalBids) {
        this.totalBids = totalBids;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}