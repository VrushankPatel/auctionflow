package com.auctionflow.api.repositories;

import com.auctionflow.api.dtos.BidHistoryDTO;
import com.auctionflow.api.dtos.UserBidsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BidReadRepositoryImpl extends JpaRepository<com.auctionflow.api.entities.Bid, Long>, BidReadRepository {

    @Query("SELECT new com.auctionflow.api.dtos.BidHistoryDTO$BidDTO(b.bidderId, b.amount, b.serverTs, b.seqNo, b.accepted) " +
           "FROM Bid b WHERE b.auctionId = :auctionId ORDER BY b.serverTs DESC")
    Page<BidHistoryDTO.BidDTO> findBidHistoryByAuctionId(@Param("auctionId") String auctionId, Pageable pageable);

    @Query("SELECT new com.auctionflow.api.dtos.UserBidsDTO$UserBidDTO(b.auctionId, i.title, b.amount, b.serverTs, b.accepted, a.status) " +
           "FROM Bid b " +
           "JOIN Auction a ON b.auctionId = a.id " +
           "JOIN Item i ON a.itemId = i.id " +
           "WHERE b.bidderId = :userId ORDER BY b.serverTs DESC")
    Page<UserBidsDTO.UserBidDTO> findBidsByUserId(@Param("userId") String userId, Pageable pageable);
}

// Note: Assuming Bid, Auction, Item entities exist.