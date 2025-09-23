package com.auctionflow.timers;

import com.auctionflow.common.service.AntiSnipeExtension;
import com.auctionflow.common.service.AuctionTimerService;
import com.auctionflow.core.domain.events.AuctionCreatedEvent;
import com.auctionflow.core.domain.events.BidPlacedEvent;
import com.auctionflow.core.domain.events.AuctionExtendedEvent;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.AuctionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuctionEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AuctionEventListener.class);

    private final com.auctionflow.common.service.AuctionTimerService timerService;
    private final AntiSnipeExtension antiSnipeExtension;

    public AuctionEventListener(com.auctionflow.common.service.AuctionTimerService timerService, AntiSnipeExtension antiSnipeExtension) {
        this.timerService = timerService;
        this.antiSnipeExtension = antiSnipeExtension;
    }

    @KafkaListener(topics = "auction-events", groupId = "timer-service")
    public void handleAuctionEvent(Object event) {
        if (event instanceof AuctionCreatedEvent auctionCreated) {
            handleAuctionCreated(auctionCreated);
        } else if (event instanceof BidPlacedEvent bidPlaced) {
            handleBidPlaced(bidPlaced);
        }
    }

    private void handleAuctionCreated(AuctionCreatedEvent event) {
        logger.info("Handling AuctionCreatedEvent for auction {}", event.getAggregateId());
        timerService.scheduleAuctionClose((AuctionId) event.getAggregateId(), event.getEndTime());
        if (event.getAuctionType() == AuctionType.DUTCH) {
            // For Dutch auctions, schedule price reductions
            // Assume interval is in the event or default
            long intervalMillis = 60000; // 1 minute default, or get from event
            timerService.schedulePriceReductions((AuctionId) event.getAggregateId(), intervalMillis, event.getEndTime());
        }
    }

    private void handleBidPlaced(BidPlacedEvent event) {
        logger.info("Handling BidPlacedEvent for auction {}", event.getAggregateId());
        // For anti-snipe, we need more info, perhaps reconstruct aggregate or have the info in event
        // For now, assume we have the necessary data
        // This is simplified; in real implementation, might need to query DB or cache
        // AuctionExtendedEvent extensionEvent = antiSnipeExtension.calculateExtensionIfNeeded(...);
        // if (extensionEvent != null) {
        //     // Publish extension event
        // }
    }
}