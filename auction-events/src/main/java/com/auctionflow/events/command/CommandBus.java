package com.auctionflow.events.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class CommandBus {

    private final ApplicationEventPublisher publisher;

    public CommandBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void send(Object command) {
        publisher.publishEvent(command);
    }
}