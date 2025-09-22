package com.auctionflow.analytics.dtos;

public class FraudScoreRequest {
    private String userId;
    private String auctionId;
    private double bidAmount;
    private int userBidHistoryCount;
    private double averageBidAmount;

    public FraudScoreRequest() {}

    public FraudScoreRequest(String userId, String auctionId, double bidAmount, int userBidHistoryCount, double averageBidAmount) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.userBidHistoryCount = userBidHistoryCount;
        this.averageBidAmount = averageBidAmount;
    }

    // getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    public int getUserBidHistoryCount() { return userBidHistoryCount; }
    public void setUserBidHistoryCount(int userBidHistoryCount) { this.userBidHistoryCount = userBidHistoryCount; }

    public double getAverageBidAmount() { return averageBidAmount; }
    public void setAverageBidAmount(double averageBidAmount) { this.averageBidAmount = averageBidAmount; }
}