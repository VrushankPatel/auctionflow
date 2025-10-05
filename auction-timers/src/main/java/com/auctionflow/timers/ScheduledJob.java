package com.auctionflow.timers;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_jobs")
public class ScheduledJob {

    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "auction_id", nullable = false)
    private String auctionId;

    @Column(name = "execute_at", nullable = false)
    private Instant executeAt;

    @Column(name = "status", nullable = false)
    private String status = "pending";

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "leased_by")
    private String leasedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public ScheduledJob() {}

    public ScheduledJob(UUID jobId, String auctionId, Instant executeAt) {
        this.jobId = jobId;
        this.auctionId = auctionId;
        this.executeAt = executeAt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public Instant getExecuteAt() {
        return executeAt;
    }

    public void setExecuteAt(Instant executeAt) {
        this.executeAt = executeAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
        this.updatedAt = Instant.now();
    }

    public Instant getLeaseUntil() {
        return leaseUntil;
    }

    public void setLeaseUntil(Instant leaseUntil) {
        this.leaseUntil = leaseUntil;
        this.updatedAt = Instant.now();
    }

    public String getLeasedBy() {
        return leasedBy;
    }

    public void setLeasedBy(String leasedBy) {
        this.leasedBy = leasedBy;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}