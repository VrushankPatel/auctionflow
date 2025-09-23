package com.auctionflow.timers;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DurableScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DurableScheduler.class);

    private final ScheduledJobRepository jobRepository;
    private final com.auctionflow.common.service.AuctionTimerService timerService;
    private final String nodeId;
    private final long leaseDurationSeconds = 300; // 5 minutes

    public DurableScheduler(ScheduledJobRepository jobRepository, com.auctionflow.common.service.AuctionTimerService timerService) {
        this.jobRepository = jobRepository;
        this.timerService = timerService;
        this.nodeId = UUID.randomUUID().toString();
    }

    @PostConstruct
    public void initialize() {
        loadAndSchedulePendingJobs();
    }

    /**
     * Schedules a durable job for auction close.
     */
    @Transactional
    public UUID scheduleAuctionClose(AuctionId auctionId, Instant executeAt) {
        // Check if job already exists
        List<ScheduledJob> existing = jobRepository.findByAuctionIdAndStatus(auctionId.value(), "pending");
        UUID jobId;
        if (!existing.isEmpty()) {
            // Update existing
            ScheduledJob job = existing.get(0);
            job.setExecuteAt(executeAt);
            job.setStatus("pending");
            job.setAttempts(0);
            job.setLeaseUntil(null);
            job.setLeasedBy(null);
            jobRepository.save(job);
            jobId = job.getJobId();
        } else {
            // Create new
            jobId = UUID.randomUUID();
            ScheduledJob job = new ScheduledJob(jobId, auctionId.value(), executeAt);
            jobRepository.save(job);
        }
        logger.info("Scheduled durable job {} for auction {} at {}", jobId, auctionId, executeAt);
        return jobId;
    }

    /**
     * Schedules durable jobs for multiple auction closes in batch.
     */
    @Transactional
    public List<UUID> scheduleAuctionCloses(List<com.auctionflow.common.service.AuctionTimerService.AuctionSchedule> schedules) {
        List<UUID> jobIds = new ArrayList<>();
        for (com.auctionflow.common.service.AuctionTimerService.AuctionSchedule schedule : schedules) {
            // Check if job already exists
            List<ScheduledJob> existing = jobRepository.findByAuctionIdAndStatus(schedule.getAuctionId().value(), "pending");
            UUID jobId;
            if (!existing.isEmpty()) {
                // Update existing
                ScheduledJob job = existing.get(0);
                job.setExecuteAt(schedule.getEndTime());
                job.setStatus("pending");
                job.setAttempts(0);
                job.setLeaseUntil(null);
                job.setLeasedBy(null);
                jobRepository.save(job);
                jobId = job.getJobId();
            } else {
                // Create new
                jobId = UUID.randomUUID();
                ScheduledJob job = new ScheduledJob(jobId, schedule.getAuctionId().value(), schedule.getEndTime());
                jobRepository.save(job);
            }
            jobIds.add(jobId);
            logger.info("Scheduled durable job {} for auction {} at {}", jobId, schedule.getAuctionId(), schedule.getEndTime());
        }
        return jobIds;
    }

    /**
     * Cancels the job for auction.
     */
    @Transactional
    public void cancelAuctionClose(AuctionId auctionId) {
        List<ScheduledJob> jobs = jobRepository.findByAuctionIdAndStatus(auctionId.value(), "pending");
        for (ScheduledJob job : jobs) {
            job.setStatus("cancelled");
            jobRepository.save(job);
        }
        timerService.cancelAuctionClose(auctionId);
        logger.info("Cancelled durable job for auction {}", auctionId);
    }

    /**
     * Loads pending jobs and acquires lease, then schedules them.
     */
    @Transactional
    public void loadAndSchedulePendingJobs() {
        Instant now = Instant.now();
        List<ScheduledJob> pendingJobs = jobRepository.findPendingJobsForRecovery("pending", now);

        for (ScheduledJob job : pendingJobs) {
            // Acquire lease
            job.setLeaseUntil(now.plusSeconds(leaseDurationSeconds));
            job.setLeasedBy(nodeId);
            jobRepository.save(job);

            // Schedule in wheel
            AuctionId auctionId = new AuctionId(job.getAuctionId());
            timerService.scheduleAuctionClose(auctionId, job.getExecuteAt());
            logger.info("Recovered and scheduled job {} for auction {}", job.getJobId(), auctionId);
        }
    }

    /**
     * Marks job as completed.
     */
    @Transactional
    public void markJobCompleted(UUID jobId) {
        ScheduledJob job = jobRepository.findById(jobId).orElse(null);
        if (job != null && nodeId.equals(job.getLeasedBy())) {
            job.setStatus("completed");
            job.setLeaseUntil(null);
            job.setLeasedBy(null);
            jobRepository.save(job);
            logger.info("Marked job {} as completed", jobId);
        }
    }

    /**
     * Handles job failure, increments attempts, releases lease.
     */
    @Transactional
    public void handleJobFailure(UUID jobId) {
        ScheduledJob job = jobRepository.findById(jobId).orElse(null);
        if (job != null && nodeId.equals(job.getLeasedBy())) {
            job.setAttempts(job.getAttempts() + 1);
            if (job.getAttempts() >= 3) {
                job.setStatus("failed");
            } else {
                job.setStatus("pending");
            }
            job.setLeaseUntil(null);
            job.setLeasedBy(null);
            jobRepository.save(job);
            logger.warn("Handled failure for job {}, attempts: {}", jobId, job.getAttempts());
        }
    }

    public String getNodeId() {
        return nodeId;
    }
}