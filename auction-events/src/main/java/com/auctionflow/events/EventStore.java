package com.auctionflow.events;

import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.util.List;

public interface EventStore {
    void save(List<DomainEvent> events, long expectedVersion);
    List<DomainEvent> getEvents(AuctionId aggregateId);
    List<DomainEvent> getEventsAfter(AuctionId aggregateId, long sequenceNumber);
}