package com.auctionflow.analytics;

import com.auctionflow.core.domain.events.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EventStreamProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EventStreamProcessor.class);

    private final MeterRegistry meterRegistry;
    private final Counter totalBids;
    private final Counter acceptedBids;
    private final AtomicInteger activeAuctions = new AtomicInteger(0);
    private final ConcurrentHashMap<Long, AtomicInteger> auctionBidCounts = new ConcurrentHashMap<>();

    public EventStreamProcessor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.totalBids = Counter.builder("auction_bids_total")
                .description("Total number of bids placed")
                .register(meterRegistry);
        this.acceptedBids = Counter.builder("auction_bids_accepted_total")
                .description("Total number of accepted bids")
                .register(meterRegistry);

        Gauge.builder("auction_active_count", activeAuctions, AtomicInteger::get)
                .description("Number of active auctions")
                .register(meterRegistry);

        Gauge.builder("auction_conversion_rate", () -> {
            double total = totalBids.count();
            double accepted = acceptedBids.count();
            return total > 0 ? accepted / total : 0.0;
        })
                .description("Conversion rate: accepted bids / total bids")
                .register(meterRegistry);

        Gauge.builder("auction_velocity", () -> {
            int active = activeAuctions.get();
            if (active == 0) return 0.0;
            return (double) auctionBidCounts.values().stream().mapToInt(AtomicInteger::get).sum() / active;
        })
                .description("Auction velocity: average bids per active auction")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "bid-events", groupId = "analytics-group")
    public void processBidEvent(DomainEvent event) {
        if (event instanceof BidPlacedEvent) {
            BidPlacedEvent bidEvent = (BidPlacedEvent) event;
            totalBids.increment();
            auctionBidCounts.computeIfAbsent(bidEvent.getAuctionId(), k -> new AtomicInteger(0)).incrementAndGet();
            logger.info("Processed BidPlacedEvent for auction {}", bidEvent.getAuctionId());
        } else if (event instanceof BidRejectedEvent) {
            // Rejected bids are still bids, but perhaps not count in accepted
        }
    }

    @KafkaListener(topics = "auction-events", groupId = "analytics-group")
    public void processAuctionEvent(DomainEvent event) {
        if (event instanceof AuctionCreatedEvent) {
            activeAuctions.incrementAndGet();
            logger.info("Auction created: {}", ((AuctionCreatedEvent) event).getAuctionId());
        } else if (event instanceof AuctionClosedEvent) {
            activeAuctions.decrementAndGet();
            logger.info("Auction closed: {}", ((AuctionClosedEvent) event).getAuctionId());
        } else if (event instanceof AuctionExtendedEvent) {
            // Extension, still active
        }
    }

    @KafkaListener(topics = "notification-events", groupId = "analytics-group")
    public void processNotificationEvent(DomainEvent event) {
        if (event instanceof WinnerDeclaredEvent) {
            acceptedBids.increment(); // Assuming the winning bid is accepted
            logger.info("Winner declared for auction {}", ((WinnerDeclaredEvent) event).getAuctionId());
        }
    }
}