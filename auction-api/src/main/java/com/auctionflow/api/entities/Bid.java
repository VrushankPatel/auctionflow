package com.auctionflow.api.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bids")
@Where(clause = "deleted_at IS NULL")
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String auctionId;
    private String bidderId;
    private BigDecimal amount;
    private Instant serverTs;
    private Long seqNo;
    private Boolean accepted;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @Column(name = "deleted_by")
    private Long deletedBy;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Instant getServerTs() { return serverTs; }
    public void setServerTs(Instant serverTs) { this.serverTs = serverTs; }

    public Long getSeqNo() { return seqNo; }
    public void setSeqNo(Long seqNo) { this.seqNo = seqNo; }

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public Long getDeletedBy() { return deletedBy; }
    public void setDeletedBy(Long deletedBy) { this.deletedBy = deletedBy; }
}