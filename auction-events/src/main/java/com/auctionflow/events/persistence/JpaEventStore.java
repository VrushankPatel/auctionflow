package com.auctionflow.events.persistence;

import com.auctionflow.common.exceptions.OptimisticLockException;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.common.service.EventStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JpaEventStore implements EventStore {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public JpaEventStore(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(List<DomainEvent> events, long expectedVersion) {
        if (!events.isEmpty()) {
            String aggregateId = ((AuctionId) events.get(0).getAggregateId()).value().toString();
            Long currentMaxSeq = eventRepository.findMaxSequenceNumberByAggregateId(aggregateId);
            long currentVersion = currentMaxSeq != null ? currentMaxSeq : 0;
            if (currentVersion != expectedVersion) {
                throw new OptimisticLockException("Version conflict: expected " + expectedVersion + ", but was " + currentVersion);
            }
        }
        List<EventEntity> entities = events.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        eventRepository.saveAll(entities);
    }

    @Override
    public List<DomainEvent> getEvents(AuctionId aggregateId) {
        return eventRepository.findByAggregateIdOrderBySequenceNumberAsc(aggregateId.value().toString())
                .stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainEvent> getEventsAfter(AuctionId aggregateId, long sequenceNumber) {
        return eventRepository.findByAggregateIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                        aggregateId.value().toString(), sequenceNumber)
                .stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainEvent> getEventsFromTimestamp(Instant fromTimestamp) {
        return eventRepository.findByTimestampGreaterThanEqualOrderByTimestampAscSequenceNumberAsc(fromTimestamp)
                .stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainEvent> getEventsForAggregateFromTimestamp(AuctionId aggregateId, Instant fromTimestamp) {
        return eventRepository.findByAggregateIdAndTimestampGreaterThanEqualOrderByTimestampAscSequenceNumberAsc(
                        aggregateId.value().toString(), fromTimestamp)
                .stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainEvent> getEventsByTimestampRange(Instant fromTimestamp, Instant toTimestamp) {
        return eventRepository.findByTimestampBetweenOrderByTimestampAscSequenceNumberAsc(fromTimestamp, toTimestamp)
                .stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
    }

    private EventEntity toEntity(DomainEvent event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            return new EventEntity(
                    ((AuctionId) event.getAggregateId()).value().toString(),
                    "auction", // aggregateType
                    event.getClass().getSimpleName(),
                    eventData,
                    null, // eventMetadata
                    event.getSequenceNumber(),
                    event.getTimestamp()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    private DomainEvent toDomainEvent(EventEntity entity) {
        try {
            // For simplicity, assume all events are in the same package
            String className = "com.auctionflow.core.domain.events." + entity.getEventType();
            Class<?> clazz = Class.forName(className);
            return (DomainEvent) objectMapper.readValue(entity.getEventData(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}