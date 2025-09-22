package com.auctionflow.api.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "archived_events")
public class ArchivedEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String aggregateId;
    private String aggregateType;
    private String eventType;
    @Lob
    private byte[] compressedEventData;
    @Lob
    private byte[] compressedEventMetadata;
    private Long sequenceNumber;
    private Instant timestamp;
    private Instant archivedAt;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public byte[] getCompressedEventData() { return compressedEventData; }
    public void setCompressedEventData(byte[] compressedEventData) { this.compressedEventData = compressedEventData; }

    public byte[] getCompressedEventMetadata() { return compressedEventMetadata; }
    public void setCompressedEventMetadata(byte[] compressedEventMetadata) { this.compressedEventMetadata = compressedEventMetadata; }

    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
}