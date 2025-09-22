package com.auctionflow.analytics.dtos;

public class BidderActivityDTO {

    private Long bidderId;
    private String bidderName;
    private Long totalBids;
    private Double winRate;
    private Long bidsToday;

    public BidderActivityDTO() {}

    public BidderActivityDTO(Long bidderId, String bidderName, Long totalBids, Double winRate, Long bidsToday) {
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.totalBids = totalBids;
        this.winRate = winRate;
        this.bidsToday = bidsToday;
    }

    // Getters and setters

    public Long getBidderId() {
        return bidderId;
    }

    public void setBidderId(Long bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public Long getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(Long totalBids) {
        this.totalBids = totalBids;
    }

    public Double getWinRate() {
        return winRate;
    }

    public void setWinRate(Double winRate) {
        this.winRate = winRate;
    }

    public Long getBidsToday() {
        return bidsToday;
    }

    public void setBidsToday(Long bidsToday) {
        this.bidsToday = bidsToday;
    }
}