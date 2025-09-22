package com.auctionflow.events.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    List<EventEntity> findByAggregateIdOrderBySequenceNumberAsc(String aggregateId);

    @Query("SELECT e FROM EventEntity e WHERE e.aggregateId = :aggregateId AND e.sequenceNumber > :sequenceNumber ORDER BY e.sequenceNumber ASC")
    List<EventEntity> findByAggregateIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(@Param("aggregateId") String aggregateId, @Param("sequenceNumber") Long sequenceNumber);

    @Query("SELECT MAX(e.sequenceNumber) FROM EventEntity e WHERE e.aggregateId = :aggregateId")
    Long findMaxSequenceNumberByAggregateId(@Param("aggregateId") String aggregateId);

    void deleteByAggregateId(String aggregateId);

    // Replay queries
    @Query("SELECT e FROM EventEntity e WHERE e.timestamp >= :fromTimestamp ORDER BY e.timestamp ASC, e.sequenceNumber ASC")
    List<EventEntity> findByTimestampGreaterThanEqualOrderByTimestampAscSequenceNumberAsc(@Param("fromTimestamp") Instant fromTimestamp);

    @Query("SELECT e FROM EventEntity e WHERE e.aggregateId = :aggregateId AND e.timestamp >= :fromTimestamp ORDER BY e.timestamp ASC, e.sequenceNumber ASC")
    List<EventEntity> findByAggregateIdAndTimestampGreaterThanEqualOrderByTimestampAscSequenceNumberAsc(@Param("aggregateId") String aggregateId, @Param("fromTimestamp") Instant fromTimestamp);

    @Query("SELECT e FROM EventEntity e WHERE e.timestamp >= :fromTimestamp AND e.timestamp <= :toTimestamp ORDER BY e.timestamp ASC, e.sequenceNumber ASC")
    List<EventEntity> findByTimestampBetweenOrderByTimestampAscSequenceNumberAsc(@Param("fromTimestamp") Instant fromTimestamp, @Param("toTimestamp") Instant toTimestamp);
}