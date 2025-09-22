package com.auctionflow.analytics.repositories;

import com.auctionflow.analytics.entities.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {

    List<AuditTrail> findByUserId(Long userId);

    List<AuditTrail> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Query("SELECT a FROM AuditTrail a WHERE a.timestamp < :cutoffDate")
    List<AuditTrail> findOldEntries(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM AuditTrail a WHERE a.timestamp < :cutoffDate")
    void deleteOldEntries(@Param("cutoffDate") LocalDateTime cutoffDate);
}