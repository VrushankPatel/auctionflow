package com.auctionflow.payments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EscrowRepository extends JpaRepository<EscrowTransaction, Long> {

    Optional<EscrowTransaction> findByAuctionId(String auctionId);

    List<EscrowTransaction> findByStatus(EscrowTransaction.EscrowStatus status);

    @Query("SELECT e FROM EscrowTransaction e WHERE e.inspectionEndTs < :now AND e.status = :status")
    List<EscrowTransaction> findExpiredInspections(@Param("now") LocalDateTime now, @Param("status") EscrowTransaction.EscrowStatus status);
}