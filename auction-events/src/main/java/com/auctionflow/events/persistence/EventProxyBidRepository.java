package com.auctionflow.events.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventProxyBidRepository extends JpaRepository<ProxyBidEntity, Long> {

    Optional<ProxyBidEntity> findByAuctionIdAndUserId(Long auctionId, java.util.UUID userId);

    List<ProxyBidEntity> findByAuctionIdAndStatus(Long auctionId, String status);

    @Query("SELECT pb FROM ProxyBidEntity pb WHERE pb.auctionId = :auctionId AND pb.status = 'ACTIVE' AND pb.maxBid > :currentHighestBid ORDER BY pb.maxBid DESC")
    List<ProxyBidEntity> findActiveProxyBidsHigherThan(@Param("auctionId") String auctionId, @Param("currentHighestBid") BigDecimal currentHighestBid);

    @Modifying
    @Query("UPDATE ProxyBidEntity pb SET pb.status = :status WHERE pb.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    @Modifying
    @Query("UPDATE ProxyBidEntity pb SET pb.currentBid = :currentBid WHERE pb.id = :id")
    void updateCurrentBid(@Param("id") Long id, @Param("currentBid") BigDecimal currentBid);
}