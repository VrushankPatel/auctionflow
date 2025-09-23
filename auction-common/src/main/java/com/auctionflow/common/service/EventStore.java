package com.auctionflow.common.service;

import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.time.Instant;
import java.util.List;

public interface EventStore {
    void save(List<DomainEvent> events, long expectedVersion);
    List<DomainEvent> getEvents(AuctionId aggregateId);
    List<DomainEvent> getEventsAfter(AuctionId aggregateId, long sequenceNumber);

    // Replay methods
    List<DomainEvent> getEventsFromTimestamp(Instant fromTimestamp);
    List<DomainEvent> getEventsForAggregateFromTimestamp(AuctionId aggregateId, Instant fromTimestamp);
    List<DomainEvent> getEventsByTimestampRange(Instant fromTimestamp, Instant toTimestamp);
}