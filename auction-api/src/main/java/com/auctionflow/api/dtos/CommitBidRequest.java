package com.auctionflow.api.dtos;

import jakarta.validation.constraints.NotNull;

public class CommitBidRequest {
    @NotNull
    private String bidHash;
    @NotNull
    private String salt;
    private String idempotencyKey;

    public CommitBidRequest() {}

    public String getBidHash() { return bidHash; }
    public void setBidHash(String bidHash) { this.bidHash = bidHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}