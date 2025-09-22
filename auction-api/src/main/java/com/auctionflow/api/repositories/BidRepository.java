package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByAuctionId(String auctionId);
    void deleteByAuctionId(String auctionId);
}