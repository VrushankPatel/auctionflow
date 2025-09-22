package com.auctionflow.events;

import com.auctionflow.core.domain.aggregates.AggregateRoot;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service for reconstructing audit trails and historical states.
 */
@Service
public class AuditTrailService {

    private final EventStore eventStore;

    public AuditTrailService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * Reconstructs the state of an aggregate as it was at a specific point in time.
     */
    public <T extends AggregateRoot> T reconstructStateAtTimestamp(AuctionId aggregateId, Instant timestamp, T aggregate) {
        // Get all events for the aggregate up to the timestamp
        List<DomainEvent> events = eventStore.getEventsForAggregateFromTimestamp(aggregateId, Instant.MIN);
        for (DomainEvent event : events) {
            if (event.getTimestamp().isBefore(timestamp) || event.getTimestamp().equals(timestamp)) {
                aggregate.apply(event);
            }
        }
        return aggregate;
    }

    /**
     * Gets the audit trail for an aggregate: all events in chronological order.
     */
    public List<DomainEvent> getAuditTrail(AuctionId aggregateId) {
        return eventStore.getEvents(aggregateId);
    }

    /**
     * Gets the audit trail for an aggregate within a time range.
     */
    public List<DomainEvent> getAuditTrailInRange(AuctionId aggregateId, Instant fromTimestamp, Instant toTimestamp) {
        List<DomainEvent> allEvents = eventStore.getEvents(aggregateId);
        return allEvents.stream()
                .filter(event -> !event.getTimestamp().isBefore(fromTimestamp) && !event.getTimestamp().isAfter(toTimestamp))
                .toList();
    }
}