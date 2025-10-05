package com.auctionflow.api.repositories;

import com.auctionflow.api.dtos.AuctionDetailsDTO;
import com.auctionflow.api.dtos.ActiveAuctionsDTO;
import com.auctionflow.api.dtos.AuctionSummaryDTO;
import com.auctionflow.api.entities.Auction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuctionReadRepository extends JpaRepository<Auction, String> {
    @Query("SELECT new com.auctionflow.api.dtos.AuctionDetailsDTO(a.id, i.id, i.sellerId, i.title, i.description, a.status, a.startTs, a.endTs, null, a.buyNowPrice, a.hiddenReserve, a.currentHighestBid, a.currentHighestBidder, a.endTs) " +
            "FROM Auction a " +
            "LEFT JOIN Item i ON a.itemId = i.id " +
            "WHERE a.id = :auctionId")
    Optional<AuctionDetailsDTO> findAuctionDetailsById(@Param("auctionId") String auctionId);

    @Query(value = "SELECT a.id, i.id, i.seller_id, i.title, i.description, a.status, a.start_ts, a.end_ts, null, a.buy_now_price, a.hidden_reserve, a.current_highest_bid, a.current_highest_bidder, a.end_ts " +
            "FROM auctions a " +
            "LEFT JOIN items i ON a.item_id = i.id " +
            "WHERE a.id = :auctionId", nativeQuery = true)
    List<Object[]> findAuctionDetailsByIdNative(@Param("auctionId") String auctionId);

    @Query(value = "SELECT a.id, a.item_id, i.seller_id, i.title, i.description, i.category_id, i.condition, " +
            "i.images, a.auction_type, a.starting_price, a.reserve_price, a.buy_now_price, " +
            "a.current_highest_bid, a.bid_count, a.hidden_reserve, a.start_ts, a.end_ts, a.status " +
            "FROM auctions a " +
            "LEFT JOIN items i ON a.item_id = i.id " +
            "WHERE a.deleted_at IS NULL AND a.status = 'OPEN' " +
            "AND (:category IS NULL OR i.category_id = :category) " +
            "AND (:sellerId IS NULL OR i.seller_id = :sellerId) " +
            "AND (:query IS NULL OR LOWER(i.title) LIKE LOWER('%' || :query || '%'))", nativeQuery = true)
    List<Object[]> findActiveAuctionsNative(@Param("category") String category, @Param("sellerId") Long sellerId, @Param("query") String query);
}