package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.BidderId;

import java.time.Instant;
import java.util.UUID;

public class BidCommittedEvent extends DomainEvent {
    private final AuctionId auctionId;
    private final BidderId bidderId;
    private final String bidHash;
    private final String salt;
    private final long commitSeqNo;

    public BidCommittedEvent(AuctionId auctionId, BidderId bidderId, String bidHash, String salt,
                             long commitSeqNo, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(eventId, timestamp, sequenceNumber);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidHash = bidHash;
        this.salt = salt;
        this.commitSeqNo = commitSeqNo;
    }

    public AuctionId getAuctionId() {
        return auctionId;
    }

    public BidderId getBidderId() {
        return bidderId;
    }

    public String getBidHash() {
        return bidHash;
    }

    public String getSalt() {
        return salt;
    }

    public long getCommitSeqNo() {
        return commitSeqNo;
    }

    @Override
    public AuctionId getAggregateId() {
        return auctionId;
    }
}