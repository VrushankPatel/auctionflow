package com.auctionflow.core.domain.aggregates;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.AuctionStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public abstract class AggregateRoot {
    protected final List<Object> domainEvents = new ArrayList<>();
    protected long version = 0;
    protected long expectedVersion = 0;

    protected void addDomainEvent(Object event) {
        domainEvents.add(event);
        version++;
    }

    public List<Object> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public long getVersion() {
        return version;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }

    public abstract Object getId();
    public abstract Object getStatus();
    public abstract Instant getEndTime();
}