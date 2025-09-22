package com.auctionflow.benchmarks;

import com.auctionflow.core.domain.aggregates.AuctionAggregate;
import com.auctionflow.core.domain.commands.PlaceBidCommand;
import com.auctionflow.core.domain.events.AuctionCreatedEvent;
import com.auctionflow.core.domain.valueobjects.*;
import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class BidPlacementBenchmark {

    private AuctionAggregate auctionAggregate;
    private PlaceBidCommand validBidCommand;

    @Setup(Level.Invocation)
    public void setup() {
        // Create an auction aggregate in OPEN state
        AuctionId auctionId = AuctionId.generate();
        ItemId itemId = new ItemId(UUID.randomUUID());
        Money reservePrice = Money.usd(java.math.BigDecimal.valueOf(10.0));
        Money buyNowPrice = Money.usd(java.math.BigDecimal.valueOf(100.0));
        Instant startTime = Instant.now().minusSeconds(60);
        Instant endTime = Instant.now().plusSeconds(300);
        AntiSnipePolicy antiSnipePolicy = AntiSnipePolicy.none();

        AuctionCreatedEvent createdEvent = new AuctionCreatedEvent(
            auctionId, itemId, UUID.randomUUID(), reservePrice, buyNowPrice, startTime, endTime, antiSnipePolicy,
            UUID.randomUUID(), Instant.now(), 1
        );

        auctionAggregate = new AuctionAggregate();
        auctionAggregate.apply(createdEvent); // Manually apply to set state

        UUID bidderId = UUID.randomUUID();
        Money bidAmount = Money.usd(java.math.BigDecimal.valueOf(50.0));
        validBidCommand = new PlaceBidCommand(auctionId, bidderId, bidAmount, "idempotency-key");
    }

    @Benchmark
    public void benchmarkPlaceBid() {
        auctionAggregate.handle(validBidCommand, Instant.now(), 1L);
    }
}