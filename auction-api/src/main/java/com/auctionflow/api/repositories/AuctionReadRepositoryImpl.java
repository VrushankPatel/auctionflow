package com.auctionflow.api.repositories;

import com.auctionflow.api.dtos.ActiveAuctionsDTO;
import com.auctionflow.api.dtos.AuctionDetailsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuctionReadRepositoryImpl extends JpaRepository<com.auctionflow.api.entities.Auction, String>, AuctionReadRepository {

    @Query("SELECT new com.auctionflow.api.dtos.AuctionDetailsDTO(a.id, i.id, i.sellerId, i.title, i.description, a.status, a.startTs, a.endTs, a.reservePrice, a.buyNowPrice, b.amount, b.bidderId, b.serverTs) " +
           "FROM Auction a " +
           "LEFT JOIN Item i ON a.itemId = i.id " +
           "LEFT JOIN Bid b ON a.id = b.auctionId AND b.accepted = true AND b.amount = (SELECT MAX(b2.amount) FROM Bid b2 WHERE b2.auctionId = a.id AND b2.accepted = true) " +
           "WHERE a.id = :auctionId")
    Optional<AuctionDetailsDTO> findAuctionDetailsById(@Param("auctionId") String auctionId);

    @Query("SELECT new com.auctionflow.api.dtos.ActiveAuctionsDTO$AuctionSummaryDTO(a.id, i.title, b.amount, a.endTs) " +
           "FROM Auction a " +
           "JOIN Item i ON a.itemId = i.id " +
           "LEFT JOIN Bid b ON a.id = b.auctionId AND b.accepted = true AND b.amount = (SELECT MAX(b2.amount) FROM Bid b2 WHERE b2.auctionId = a.id AND b2.accepted = true) " +
           "WHERE a.status = 'ACTIVE' " +
           "AND (:category IS NULL OR i.categoryId = :category) " +
           "AND (:sellerId IS NULL OR i.sellerId = :sellerId)")
    Page<ActiveAuctionsDTO.AuctionSummaryDTO> findActiveAuctions(@Param("category") String category, @Param("sellerId") String sellerId, Pageable pageable);
}

// Note: Assuming Auction, Item, Bid entities exist in core or common.