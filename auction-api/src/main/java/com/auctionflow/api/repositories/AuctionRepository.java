package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, String> {
    List<Auction> findByStatusAndEndTsBefore(String status, Instant endTs);
}