package com.auctionflow.core.domain.valueobjects;

import java.time.Instant;
import java.util.Objects;

public class SealedBidCommit {
    private final BidderId bidderId;
    private final String hash;
    private final String salt;
    private final Instant timestamp;
    private final long seqNo;

    public SealedBidCommit(BidderId bidderId, String hash, String salt, Instant timestamp, long seqNo) {
        this.bidderId = bidderId;
        this.hash = hash;
        this.salt = salt;
        this.timestamp = timestamp;
        this.seqNo = seqNo;
    }

    public BidderId getBidderId() {
        return bidderId;
    }

    public String getHash() {
        return hash;
    }

    public String getSalt() {
        return salt;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getSeqNo() {
        return seqNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SealedBidCommit that = (SealedBidCommit) o;
        return seqNo == that.seqNo &&
                Objects.equals(bidderId, that.bidderId) &&
                Objects.equals(hash, that.hash) &&
                Objects.equals(salt, that.salt) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bidderId, hash, salt, timestamp, seqNo);
    }

    @Override
    public String toString() {
        return "SealedBidCommit{" +
                "bidderId=" + bidderId +
                ", hash='" + hash + '\'' +
                ", salt='" + salt + '\'' +
                ", timestamp=" + timestamp +
                ", seqNo=" + seqNo +
                '}';
    }
}