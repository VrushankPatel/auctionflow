package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Optional<Dispute> findByAuctionId(String auctionId);

    List<Dispute> findByInitiatorId(String initiatorId);

    List<Dispute> findByStatus(String status);

    @Query("SELECT d FROM Dispute d WHERE d.auctionId = :auctionId AND d.status != 'CLOSED'")
    Optional<Dispute> findOpenDisputeByAuctionId(@Param("auctionId") String auctionId);
}