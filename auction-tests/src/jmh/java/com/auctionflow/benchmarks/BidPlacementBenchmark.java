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
        SellerId sellerId = new SellerId(UUID.randomUUID());
        String categoryId = "test";
        AuctionType auctionType = AuctionType.ENGLISH_OPEN;
        Money reservePrice = Money.usd(java.math.BigDecimal.valueOf(10.0));
        Money buyNowPrice = Money.usd(java.math.BigDecimal.valueOf(100.0));
        Instant startTime = Instant.now().minusSeconds(60);
        Instant endTime = Instant.now().plusSeconds(300);
        AntiSnipePolicy antiSnipePolicy = AntiSnipePolicy.none();
        boolean hiddenReserve = false;
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        long sequenceNumber = 1;

        AuctionCreatedEvent createdEvent = new AuctionCreatedEvent(
            auctionId, itemId, sellerId, categoryId, auctionType, reservePrice, buyNowPrice, startTime, endTime, antiSnipePolicy, hiddenReserve,
            eventId, timestamp, sequenceNumber
        );

        auctionAggregate = new AuctionAggregate();
        auctionAggregate.apply(createdEvent); // Manually apply to set state

        UUID bidderId = UUID.randomUUID();
        Money bidAmount = Money.usd(java.math.BigDecimal.valueOf(50.0));
        Instant serverTs = Instant.now();
        long seqNo = 2;
        validBidCommand = new PlaceBidCommand(auctionId, bidderId, bidAmount, "idempotency-key", serverTs, seqNo);
    }

    @Benchmark
    public void benchmarkPlaceBid() {
        auctionAggregate.handle(validBidCommand);
    }
}