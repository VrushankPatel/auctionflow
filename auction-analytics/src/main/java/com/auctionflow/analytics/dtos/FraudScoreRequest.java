package com.auctionflow.analytics.dtos;

public class FraudScoreRequest {
    private String userId;
    private String auctionId;
    private double bidAmount;
    private int userBidHistoryCount;
    private double averageBidAmount;
    private long bidsLastHour;
    private long bidsLastMinute;
    private double bidAmountStdDev;
    private long timeSinceLastBid; // in seconds

    public FraudScoreRequest() {}

    public FraudScoreRequest(String userId, String auctionId, double bidAmount, int userBidHistoryCount, double averageBidAmount,
                             long bidsLastHour, long bidsLastMinute, double bidAmountStdDev, long timeSinceLastBid) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.userBidHistoryCount = userBidHistoryCount;
        this.averageBidAmount = averageBidAmount;
        this.bidsLastHour = bidsLastHour;
        this.bidsLastMinute = bidsLastMinute;
        this.bidAmountStdDev = bidAmountStdDev;
        this.timeSinceLastBid = timeSinceLastBid;
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

    public long getBidsLastHour() { return bidsLastHour; }
    public void setBidsLastHour(long bidsLastHour) { this.bidsLastHour = bidsLastHour; }

    public long getBidsLastMinute() { return bidsLastMinute; }
    public void setBidsLastMinute(long bidsLastMinute) { this.bidsLastMinute = bidsLastMinute; }

    public double getBidAmountStdDev() { return bidAmountStdDev; }
    public void setBidAmountStdDev(double bidAmountStdDev) { this.bidAmountStdDev = bidAmountStdDev; }

    public long getTimeSinceLastBid() { return timeSinceLastBid; }
    public void setTimeSinceLastBid(long timeSinceLastBid) { this.timeSinceLastBid = timeSinceLastBid; }
}