package com.auctionflow.api.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_status")
public class RefreshStatus {
    @Id
    private String projectionName;
    private String lastEventId;
    private Instant lastProcessedAt;

    // getters and setters
    public String getProjectionName() { return projectionName; }
    public void setProjectionName(String projectionName) { this.projectionName = projectionName; }

    public String getLastEventId() { return lastEventId; }
    public void setLastEventId(String lastEventId) { this.lastEventId = lastEventId; }

    public Instant getLastProcessedAt() { return lastProcessedAt; }
    public void setLastProcessedAt(Instant lastProcessedAt) { this.lastProcessedAt = lastProcessedAt; }
}