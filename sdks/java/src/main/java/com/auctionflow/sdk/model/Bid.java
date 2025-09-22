package com.auctionflow.sdk.model;

import java.math.BigDecimal;
import java.time.Instant;

public class Bid {
    private String id;
    private String bidderId;
    private BigDecimal amount;
    private Instant timestamp;
    private boolean accepted;

    public Bid() {}

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
}