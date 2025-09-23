package com.auctionflow.common.service;

import com.auctionflow.core.domain.events.DomainEvent;

public interface EventPublisher {
    void publish(DomainEvent event);
}