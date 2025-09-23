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

    @Query("SELECT new com.auctionflow.api.dtos.AuctionDetailsDTO(a.id, i.id, i.sellerId, i.title, i.description, a.status, a.startTs, a.endTs, null, a.buyNowPrice, a.hiddenReserve, a.currentHighestBid, a.currentHighestBidder, a.endTs) " +
            "FROM Auction a " +
            "LEFT JOIN Item i ON a.itemId = i.id " +
            "WHERE a.id = :auctionId")
    Optional<AuctionDetailsDTO> findAuctionDetailsById(@Param("auctionId") String auctionId);

    @Query("SELECT new com.auctionflow.api.dtos.ActiveAuctionsDTO$AuctionSummaryDTO(a.id, i.title, a.currentHighestBid, a.endTs) " +
            "FROM Auction a " +
            "JOIN Item i ON a.itemId = i.id " +
            "WHERE a.status = 'OPEN' " +
            "AND (:category IS NULL OR i.categoryId = :category) " +
            "AND (:sellerId IS NULL OR i.sellerId = :sellerId) " +
            "AND (:query IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<ActiveAuctionsDTO.AuctionSummaryDTO> findActiveAuctions(@Param("category") String category, @Param("sellerId") String sellerId, @Param("query") String query, Pageable pageable);
}

// Note: Assuming Auction, Item, Bid entities exist in core or common.