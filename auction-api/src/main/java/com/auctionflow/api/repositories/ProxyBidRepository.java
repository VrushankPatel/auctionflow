package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.ProxyBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProxyBidRepository extends JpaRepository<ProxyBid, Long> {

    Optional<ProxyBid> findByAuctionIdAndUserId(String auctionId, UUID userId);

    List<ProxyBid> findByAuctionIdAndStatus(String auctionId, String status);

    @Query("SELECT pb FROM ProxyBid pb WHERE pb.auctionId = :auctionId AND pb.status = 'ACTIVE' AND pb.maxBid > :currentHighestBid ORDER BY pb.maxBid DESC")
    List<ProxyBid> findActiveProxyBidsHigherThan(@Param("auctionId") String auctionId, @Param("currentHighestBid") BigDecimal currentHighestBid);

    @Modifying
    @Query("UPDATE ProxyBid pb SET pb.status = :status WHERE pb.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    @Modifying
    @Query("UPDATE ProxyBid pb SET pb.currentBid = :currentBid WHERE pb.id = :id")
    void updateCurrentBid(@Param("id") Long id, @Param("currentBid") BigDecimal currentBid);
}