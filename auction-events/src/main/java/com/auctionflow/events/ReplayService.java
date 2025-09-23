package com.auctionflow.events;

import com.auctionflow.common.service.EventStore;
import com.auctionflow.core.domain.aggregates.AggregateRoot;
import com.auctionflow.core.domain.events.CompensationEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ReplayService {

    private final EventStore eventStore;

    public ReplayService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * Rebuilds an aggregate by replaying all its events.
     */
    public <T extends AggregateRoot> T rebuildAggregate(AuctionId aggregateId, T aggregate) {
        List<DomainEvent> events = eventStore.getEvents(aggregateId);
        for (DomainEvent event : events) {
            aggregate.apply(event);
        }
        return aggregate;
    }

    /**
     * Rebuilds an aggregate by replaying events from a specific timestamp.
     */
    public <T extends AggregateRoot> T rebuildAggregateFromTimestamp(AuctionId aggregateId, Instant fromTimestamp, T aggregate) {
        List<DomainEvent> events = eventStore.getEventsForAggregateFromTimestamp(aggregateId, fromTimestamp);
        for (DomainEvent event : events) {
            aggregate.apply(event);
        }
        return aggregate;
    }

    /**
     * Replays events globally from a timestamp, useful for rebuilding multiple aggregates or for audit.
     */
    public List<DomainEvent> replayEventsFromTimestamp(Instant fromTimestamp) {
        return eventStore.getEventsFromTimestamp(fromTimestamp);
    }

    /**
     * Replays events within a timestamp range.
     */
    public List<DomainEvent> replayEventsInRange(Instant fromTimestamp, Instant toTimestamp) {
        return eventStore.getEventsByTimestampRange(fromTimestamp, toTimestamp);
    }

    /**
     * Applies compensation events to an aggregate.
     */
    public <T extends AggregateRoot> T applyCompensation(T aggregate, List<CompensationEvent> compensationEvents) {
        for (CompensationEvent event : compensationEvents) {
            event.compensate(aggregate);
        }
        return aggregate;
    }
}