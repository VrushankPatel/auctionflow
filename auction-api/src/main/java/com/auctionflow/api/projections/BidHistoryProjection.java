package com.auctionflow.api.projections;

import com.auctionflow.api.entities.BidHistory;
import com.auctionflow.api.entities.RefreshStatus;
import com.auctionflow.api.repositories.BidHistoryRepository;
import com.auctionflow.api.repositories.RefreshStatusRepository;
import com.auctionflow.core.domain.events.BidPlacedEvent;
import com.auctionflow.core.domain.events.EventHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class BidHistoryProjection {

    private final BidHistoryRepository bidHistoryRepository;
    private final RefreshStatusRepository refreshStatusRepository;

    public BidHistoryProjection(BidHistoryRepository bidHistoryRepository,
                               RefreshStatusRepository refreshStatusRepository) {
        this.bidHistoryRepository = bidHistoryRepository;
        this.refreshStatusRepository = refreshStatusRepository;
    }

    @EventHandler
    @Async
    @Transactional
    public void on(BidPlacedEvent event) {
        updateBidHistory(event);
        updateRefreshStatus(event, "BidHistoryProjection");
    }

    private void updateBidHistory(BidPlacedEvent event) {
        BidHistory bidHistory = new BidHistory();
        bidHistory.setAuctionId(event.getAggregateId().toString());
        bidHistory.setBidderId(event.getBidderId().toString());
        bidHistory.setAmount(event.getAmount().toBigDecimal());
        bidHistory.setServerTs(event.getTimestamp());
        bidHistory.setSeqNo(event.getSequenceNumber());
        bidHistory.setAccepted(true); // Assume accepted for now

        bidHistoryRepository.save(bidHistory);
    }

    private void updateRefreshStatus(BidPlacedEvent event, String projectionName) {
        RefreshStatus status = refreshStatusRepository.findById(projectionName).orElse(new RefreshStatus());
        status.setProjectionName(projectionName);
        status.setLastEventId(event.getEventId().toString());
        status.setLastProcessedAt(Instant.now());
        refreshStatusRepository.save(status);
    }
}