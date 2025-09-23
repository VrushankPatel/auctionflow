package com.auctionflow.bidding.strategies;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;

public class AutomatedBidStrategy {
    private final String id;
    private final AuctionId auctionId;
    private final BidderId bidderId;
    private final StrategyType strategyType;

    public AutomatedBidStrategy(String id, AuctionId auctionId, BidderId bidderId, StrategyType strategyType) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.strategyType = strategyType;
    }

    public String getId() { return id; }
    public AuctionId getAuctionId() { return auctionId; }
    public BidderId getBidderId() { return bidderId; }
    public StrategyType getStrategyType() { return strategyType; }
}