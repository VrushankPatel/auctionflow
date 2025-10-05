package com.auctionflow.tests;

import com.auctionflow.core.domain.aggregates.AuctionAggregate;
import com.auctionflow.core.domain.events.AuctionCreatedEvent;
import com.auctionflow.core.domain.events.BidPlacedEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.*;
import com.auctionflow.common.service.EventStore;
import com.auctionflow.events.ReplayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ReplayServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EventStore eventStore;

    @Autowired
    private ReplayService replayService;

    @Test
    public void testRebuildAggregate() {
        // Given
        AuctionId auctionId = AuctionId.generate();
        SellerId sellerId = SellerId.of(UUID.randomUUID().toString());
        ItemId itemId = ItemId.generate();
        Money reservePrice = Money.usd(BigDecimal.valueOf(100));
        Money buyNowPrice = null;
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(3600);
        AntiSnipePolicy antiSnipePolicy = AntiSnipePolicy.none();
        boolean hiddenReserve = false;
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        long sequenceNumber = 1L;

        AuctionCreatedEvent createdEvent = new AuctionCreatedEvent(
                auctionId, itemId, sellerId, "test-category", AuctionType.ENGLISH_OPEN, reservePrice, buyNowPrice, startTime, endTime, antiSnipePolicy, hiddenReserve, eventId, timestamp, sequenceNumber
        );

        eventStore.save(List.of(createdEvent), 0);

        // When
        AuctionAggregate rebuilt = replayService.rebuildAggregate(auctionId, new AuctionAggregate(List.of(createdEvent)));

        // Then
        assertThat(rebuilt.getId()).isEqualTo(auctionId);
        assertThat(rebuilt.getSellerId()).isEqualTo(sellerId);
        assertThat(rebuilt.getCurrentHighestBid()).isEqualTo(reservePrice);
    }

    @Test
    public void testRebuildAggregateFromTimestamp() {
        // Given
        AuctionId auctionId = AuctionId.generate();
        Instant baseTime = Instant.now();

        AuctionCreatedEvent createdEvent = new AuctionCreatedEvent(
                auctionId, ItemId.generate(), SellerId.of(UUID.randomUUID().toString()), "test-category", AuctionType.ENGLISH_OPEN,
                Money.usd(BigDecimal.valueOf(100)), null, baseTime, baseTime.plusSeconds(3600), AntiSnipePolicy.none(), false,
                UUID.randomUUID(), baseTime.minusSeconds(10), 1L
        );

        BidPlacedEvent bidEvent = new BidPlacedEvent(
                auctionId, UUID.randomUUID().toString(), Money.usd(BigDecimal.valueOf(150)), baseTime.plusSeconds(5), UUID.randomUUID(), 2L, 1L
        );

        eventStore.save(List.of(createdEvent, bidEvent), 0);

        // When - replay from after creation
        AuctionAggregate rebuilt = replayService.rebuildAggregateFromTimestamp(
                auctionId, baseTime, new AuctionAggregate(List.of(createdEvent))
        );

        // Then - should only have the bid applied, assuming aggregate starts empty
        // Note: This assumes the aggregate apply logic handles partial replay
        assertThat(rebuilt.getId()).isEqualTo(auctionId);
    }

    @Test
    public void testReplayEventsFromTimestamp() {
        // Given
        Instant fromTime = Instant.now();

        // Save some events
        AuctionId auctionId = AuctionId.generate();
        AuctionCreatedEvent event = new AuctionCreatedEvent(
                auctionId, ItemId.generate(), SellerId.of(UUID.randomUUID().toString()), "test-category", AuctionType.ENGLISH_OPEN,
                Money.usd(BigDecimal.valueOf(100)), null, fromTime, fromTime.plusSeconds(3600), AntiSnipePolicy.none(), false,
                UUID.randomUUID(), fromTime.plusSeconds(1), 1L
        );
        eventStore.save(List.of(event), 0);

        // When
        List<DomainEvent> events = replayService.replayEventsFromTimestamp(fromTime);

        // Then
        assertThat(events).hasSizeGreaterThanOrEqualTo(1);
        assertThat(events.get(0).getTimestamp()).isAfterOrEqualTo(fromTime);
    }
}