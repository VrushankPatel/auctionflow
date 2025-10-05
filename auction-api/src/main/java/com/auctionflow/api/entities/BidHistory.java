package com.auctionflow.api.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bid_history")
public class BidHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_id")
    private String auctionId;

    @Column(name = "bidder_id")
    private String bidderId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "server_ts")
    private Instant serverTs;

    @Column(name = "seq_no")
    private Long seqNo;

    @Column(name = "accepted")
    private Boolean accepted;

    // Constructors
    public BidHistory() {}

    public BidHistory(String auctionId, String bidderId, BigDecimal amount,
                     Instant serverTs, Long seqNo, Boolean accepted) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.serverTs = serverTs;
        this.seqNo = seqNo;
        this.accepted = accepted;
    }

    // Getters and setters
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
}