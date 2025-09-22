package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.ArchivedBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchivedBidRepository extends JpaRepository<ArchivedBid, Long> {
    List<ArchivedBid> findByAuctionId(String auctionId);
}