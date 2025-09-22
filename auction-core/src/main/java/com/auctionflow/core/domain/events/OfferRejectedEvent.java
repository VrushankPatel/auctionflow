package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.OfferId;

import java.time.Instant;
import java.util.UUID;

public class OfferRejectedEvent extends DomainEvent {
    public OfferRejectedEvent(OfferId offerId, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(offerId, eventId, timestamp, sequenceNumber);
    }
}