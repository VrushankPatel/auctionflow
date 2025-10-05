package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.BidHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidHistoryRepository extends JpaRepository<BidHistory, Long> {

    List<BidHistory> findByAuctionIdOrderByServerTsDesc(String auctionId);

    List<BidHistory> findByBidderIdOrderByServerTsDesc(String bidderId);
}