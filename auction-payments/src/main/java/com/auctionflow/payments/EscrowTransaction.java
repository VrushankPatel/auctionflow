package com.auctionflow.payments;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "escrow_transactions")
public class EscrowTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private String auctionId;

    @Column(name = "winner_id", nullable = false)
    private String winnerId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscrowStatus status = EscrowStatus.AUTHORIZED;

    @Column(name = "authorization_id")
    private String authorizationId;

    @Column(name = "capture_id")
    private String captureId;

    @Column(name = "inspection_end_ts")
    private LocalDateTime inspectionEndTs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Constructors
    public EscrowTransaction() {}

    public EscrowTransaction(String auctionId, String winnerId, BigDecimal amount, String authorizationId) {
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.amount = amount;
        this.authorizationId = authorizationId;
        this.status = EscrowStatus.AUTHORIZED;
        this.inspectionEndTs = LocalDateTime.now().plusDays(3); // Example: 3 days inspection
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public EscrowStatus getStatus() {
        return status;
    }

    public void setStatus(EscrowStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }

    public String getCaptureId() {
        return captureId;
    }

    public void setCaptureId(String captureId) {
        this.captureId = captureId;
    }

    public LocalDateTime getInspectionEndTs() {
        return inspectionEndTs;
    }

    public void setInspectionEndTs(LocalDateTime inspectionEndTs) {
        this.inspectionEndTs = inspectionEndTs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum EscrowStatus {
        AUTHORIZED,
        INSPECTION,
        CAPTURED,
        CANCELLED
    }
}