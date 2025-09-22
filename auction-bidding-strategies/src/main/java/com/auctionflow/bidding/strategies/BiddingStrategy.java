package com.auctionflow.bidding.strategies;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;

import java.time.Instant;

/**
 * Base interface for automated bidding strategies
 */
public interface BiddingStrategy {

    /**
     * Determine if a bid should be placed and at what amount
     * @param auctionId The auction ID
     * @param bidderId The bidder ID
     * @param currentHighestBid Current highest bid
     * @param auctionEndTime When the auction ends
     * @param strategyParams Strategy-specific parameters
     * @return BidDecision with amount and timing
     */
    BidDecision decideBid(AuctionId auctionId, BidderId bidderId, Money currentHighestBid,
                          Instant auctionEndTime, StrategyParameters strategyParams);

    /**
     * Get the strategy type
     */
    StrategyType getType();
}