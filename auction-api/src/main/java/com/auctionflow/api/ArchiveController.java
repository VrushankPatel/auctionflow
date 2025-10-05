package com.auctionflow.api;

import com.auctionflow.api.entities.ArchivedAuction;
import com.auctionflow.api.entities.ArchivedBid;
import com.auctionflow.api.entities.ArchivedEvent;
import com.auctionflow.api.repositories.ArchivedAuctionRepository;
import com.auctionflow.api.repositories.ArchivedBidRepository;
import com.auctionflow.api.repositories.ArchivedEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

@RestController
@RequestMapping("/api/v1/archive")
@Profile("!ui-only")
public class ArchiveController {

    @Autowired
    private ArchivedAuctionRepository archivedAuctionRepository;

    @Autowired
    private ArchivedBidRepository archivedBidRepository;

    @Autowired
    private ArchivedEventRepository archivedEventRepository;

    @GetMapping("/auctions/{id}")
    public ResponseEntity<ArchivedAuction> getArchivedAuction(@PathVariable String id) {
        Optional<ArchivedAuction> auction = archivedAuctionRepository.findById(id);
        return auction.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/auctions/{id}/bids")
    public ResponseEntity<List<ArchivedBid>> getArchivedBids(@PathVariable String id) {
        List<ArchivedBid> bids = archivedBidRepository.findByAuctionId(id);
        return ResponseEntity.ok(bids);
    }

    @GetMapping("/auctions/{id}/events")
    public ResponseEntity<List<ArchivedEventDTO>> getArchivedEvents(@PathVariable String id) {
        List<ArchivedEvent> events = archivedEventRepository.findByAggregateId(id);
        List<ArchivedEventDTO> dtos = events.stream().map(this::mapToDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    private ArchivedEventDTO mapToDTO(ArchivedEvent event) {
        ArchivedEventDTO dto = new ArchivedEventDTO();
        dto.setId(event.getId());
        dto.setAggregateId(event.getAggregateId());
        dto.setAggregateType(event.getAggregateType());
        dto.setEventType(event.getEventType());
        try {
            dto.setEventData(decompress(event.getCompressedEventData()));
            if (event.getCompressedEventMetadata() != null) {
                dto.setEventMetadata(decompress(event.getCompressedEventMetadata()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to decompress event data", e);
        }
        dto.setSequenceNumber(event.getSequenceNumber());
        dto.setTimestamp(event.getTimestamp());
        dto.setArchivedAt(event.getArchivedAt());
        return dto;
    }

    private String decompress(byte[] compressed) throws IOException {
        if (compressed == null) return null;
        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
            return new String(gzis.readAllBytes(), "UTF-8");
        }
    }

    public static class ArchivedEventDTO {
        private Long id;
        private String aggregateId;
        private String aggregateType;
        private String eventType;
        private String eventData;
        private String eventMetadata;
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

        public String getEventData() { return eventData; }
        public void setEventData(String eventData) { this.eventData = eventData; }

        public String getEventMetadata() { return eventMetadata; }
        public void setEventMetadata(String eventMetadata) { this.eventMetadata = eventMetadata; }

        public Long getSequenceNumber() { return sequenceNumber; }
        public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

        public Instant getArchivedAt() { return archivedAt; }
        public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    }
}