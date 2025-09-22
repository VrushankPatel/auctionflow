package com.auctionflow.analytics.repositories;

import com.auctionflow.analytics.entities.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByAuctionId(Long auctionId);

    @Query("SELECT b FROM Bid b WHERE b.accepted = true AND DATE(b.serverTs) = DATE(:date)")
    List<Bid> findAcceptedBidsByDate(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(b) FROM Bid b WHERE b.bidderId = :bidderId AND DATE(b.serverTs) = DATE(:date)")
    Long countBidsByBidderAndDate(@Param("bidderId") Long bidderId, @Param("date") LocalDateTime date);
}