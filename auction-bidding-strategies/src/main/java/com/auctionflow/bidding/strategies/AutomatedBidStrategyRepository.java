package com.auctionflow.bidding.strategies;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for automated bid strategies
 */
public interface AutomatedBidStrategyRepository extends JpaRepository<AutomatedBidStrategy, Long> {

    @Query("SELECT s FROM AutomatedBidStrategy s WHERE s.auctionId = :auctionId AND s.isActive = true")
    List<AutomatedBidStrategy> findActiveByAuctionId(@Param("auctionId") Long auctionId);

    List<AutomatedBidStrategy> findByBidderIdAndIsActive(String bidderId, boolean isActive);

    List<AutomatedBidStrategy> findByBidderId(String bidderId);
}