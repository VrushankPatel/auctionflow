package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.AuctionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionDetailsRepository extends JpaRepository<AuctionDetails, String> {

    @Query("SELECT ad FROM AuctionDetails ad WHERE ad.status = 'ACTIVE' ORDER BY ad.endTs ASC")
    List<AuctionDetails> findActiveAuctions();

    @Query("SELECT ad FROM AuctionDetails ad WHERE ad.sellerId = :sellerId")
    List<AuctionDetails> findBySellerId(@Param("sellerId") String sellerId);

    @Query("SELECT ad FROM AuctionDetails ad WHERE ad.category = :category AND ad.status = 'ACTIVE'")
    List<AuctionDetails> findByCategory(@Param("category") String category);

    Optional<AuctionDetails> findByAuctionId(String auctionId);
}