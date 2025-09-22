package com.auctionflow.analytics.repositories;

import com.auctionflow.analytics.entities.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByStatusAndEndTsBetween(String status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND DATE(a.endTs) = DATE(:date)")
    List<Auction> findClosedAuctionsByDate(@Param("status") String status, @Param("date") LocalDateTime date);

    List<Auction> findByItemIdIn(List<Long> itemIds);
}