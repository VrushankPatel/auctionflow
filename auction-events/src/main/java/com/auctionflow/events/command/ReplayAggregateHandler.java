package com.auctionflow.events.command;

import com.auctionflow.core.domain.aggregates.AuctionAggregate;
import com.auctionflow.core.domain.commands.ReplayAggregateCommand;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.events.ReplayService;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Handler for replaying and rebuilding aggregates.
 */
@Component
public class ReplayAggregateHandler {

    private final ReplayService replayService;

    public ReplayAggregateHandler(ReplayService replayService) {
        this.replayService = replayService;
    }

    public AuctionAggregate handle(ReplayAggregateCommand command) {
        AuctionId aggregateId = command.aggregateId();
        Instant fromTimestamp = command.fromTimestamp();

        AuctionAggregate aggregate = new AuctionAggregate();

        if (fromTimestamp != null) {
            return replayService.rebuildAggregateFromTimestamp(aggregateId, fromTimestamp, aggregate);
        } else {
            return replayService.rebuildAggregate(aggregateId, aggregate);
        }
    }
}