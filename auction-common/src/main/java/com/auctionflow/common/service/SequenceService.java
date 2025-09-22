package com.auctionflow.common.service;

import com.auctionflow.core.domain.valueobjects.AuctionId;

/**
 * Service for generating globally monotonic sequence numbers for auctions.
 * Ensures strict ordering in distributed environments for fairness in high-frequency bidding.
 */
public interface SequenceService {

    /**
     * Generates the next sequence number for the given auction.
     * Uses Redis atomic increment for distributed consistency.
     *
     * @param auctionId the auction identifier
     * @return the next sequence number
     */
    long nextSequence(AuctionId auctionId);
}