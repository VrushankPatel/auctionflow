package com.auctionflow.core.domain.events;

import java.time.Instant;
import java.util.UUID;

public class DisputeResolvedEvent extends DomainEvent {
    private final Long disputeId;
    private final String auctionId;
    private final String resolverId;
    private final String resolutionNotes;
    private final boolean captured;

    public DisputeResolvedEvent(Long disputeId, String auctionId, String resolverId, String resolutionNotes, boolean captured, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(null, eventId, timestamp, sequenceNumber);
        this.disputeId = disputeId;
        this.auctionId = auctionId;
        this.resolverId = resolverId;
        this.resolutionNotes = resolutionNotes;
        this.captured = captured;
    }

    public Long getDisputeId() {
        return disputeId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getResolverId() {
        return resolverId;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public boolean isCaptured() {
        return captured;
    }
}