package com.auctionflow.events.command;

public interface CommandHandler<T> {
    void handle(T command);
}