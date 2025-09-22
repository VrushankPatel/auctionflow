package com.auctionflow.core.domain.aggregates;

import java.util.ArrayList;
import java.util.List;

public abstract class AggregateRoot {
    protected final List<Object> domainEvents = new ArrayList<>();
    protected long version = 0;

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
}