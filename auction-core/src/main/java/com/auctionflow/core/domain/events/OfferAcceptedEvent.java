package com.auctionflow.core.domain.events;

import com.auctionflow.core.domain.valueobjects.OfferId;

import java.time.Instant;
import java.util.UUID;

public class OfferAcceptedEvent extends DomainEvent {
    public OfferAcceptedEvent(OfferId offerId, UUID eventId, Instant timestamp, long sequenceNumber) {
        super(offerId, eventId, timestamp, sequenceNumber);
    }
}