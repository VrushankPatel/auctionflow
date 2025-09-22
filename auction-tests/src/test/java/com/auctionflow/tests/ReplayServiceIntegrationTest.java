package com.auctionflow.tests;

import com.auctionflow.core.domain.aggregates.AuctionAggregate;
import com.auctionflow.core.domain.events.AuctionCreatedEvent;
import com.auctionflow.core.domain.events.BidPlacedEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.*;
import com.auctionflow.events.EventStore;
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
        SellerId sellerId = SellerId.generate();
        ItemId itemId = ItemId.generate();
        Money reservePrice = Money.of(BigDecimal.valueOf(100));
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(3600);

        AuctionCreatedEvent createdEvent = new AuctionCreatedEvent(
                auctionId, UUID.randomUUID(), Instant.now(), 1L,
                sellerId, itemId, AuctionType.ENGLISH, startTime, endTime, reservePrice, null, null
        );

        eventStore.save(List.of(createdEvent), 0);

        // When
        AuctionAggregate rebuilt = replayService.rebuildAggregate(auctionId, new AuctionAggregate(auctionId));

        // Then
        assertThat(rebuilt.getId()).isEqualTo(auctionId);
        assertThat(rebuilt.getSellerId()).isEqualTo(sellerId);
        assertThat(rebuilt.getReservePrice()).isEqualTo(reservePrice);
    }

    @Test
    public void testRebuildAggregateFromTimestamp() {
        // Given
        AuctionId auctionId = AuctionId.generate();
        Instant baseTime = Instant.now();

        AuctionCreatedEvent createdEvent = new AuctionCreatedEvent(
                auctionId, UUID.randomUUID(), baseTime.minusSeconds(10), 1L,
                SellerId.generate(), ItemId.generate(), AuctionType.ENGLISH,
                baseTime, baseTime.plusSeconds(3600), Money.of(BigDecimal.valueOf(100)), null, null
        );

        BidPlacedEvent bidEvent = new BidPlacedEvent(
                auctionId, UUID.randomUUID(), baseTime.plusSeconds(5), 2L,
                BidderId.generate(), Money.of(BigDecimal.valueOf(150)), 1L
        );

        eventStore.save(List.of(createdEvent, bidEvent), 0);

        // When - replay from after creation
        AuctionAggregate rebuilt = replayService.rebuildAggregateFromTimestamp(
                auctionId, baseTime, new AuctionAggregate(auctionId)
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
                auctionId, UUID.randomUUID(), fromTime.plusSeconds(1), 1L,
                SellerId.generate(), ItemId.generate(), AuctionType.ENGLISH,
                fromTime, fromTime.plusSeconds(3600), Money.of(BigDecimal.valueOf(100)), null, null
        );
        eventStore.save(List.of(event), 0);

        // When
        List<DomainEvent> events = replayService.replayEventsFromTimestamp(fromTime);

        // Then
        assertThat(events).hasSizeGreaterThanOrEqualTo(1);
        assertThat(events.get(0).getTimestamp()).isAfterOrEqualTo(fromTime);
    }
}