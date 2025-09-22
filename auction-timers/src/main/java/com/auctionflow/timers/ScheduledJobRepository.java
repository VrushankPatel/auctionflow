package com.auctionflow.timers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, UUID> {

    List<ScheduledJob> findByStatusAndExecuteAtBefore(String status, Instant now);

    @Query("SELECT j FROM ScheduledJob j WHERE j.status = :status AND (j.leaseUntil IS NULL OR j.leaseUntil < :now)")
    List<ScheduledJob> findPendingJobsForRecovery(@Param("status") String status, @Param("now") Instant now);

    List<ScheduledJob> findByAuctionIdAndStatus(Long auctionId, String status);
}