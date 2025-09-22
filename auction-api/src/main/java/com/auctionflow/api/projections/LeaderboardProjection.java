package com.auctionflow.api.projections;

import com.auctionflow.api.entities.RefreshStatus;
import com.auctionflow.api.repositories.RefreshStatusRepository;
import com.auctionflow.core.domain.events.BidPlacedEvent;
import com.auctionflow.core.domain.events.EventHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class LeaderboardProjection {

    private final RefreshStatusRepository refreshStatusRepository;

    public LeaderboardProjection(RefreshStatusRepository refreshStatusRepository) {
        this.refreshStatusRepository = refreshStatusRepository;
    }

    @EventHandler
    @Async
    @Transactional
    public void on(BidPlacedEvent event) {
        // Eventual update for analytics
        updateLeaderboard(event);
        updateRefreshStatus(event, "LeaderboardProjection");
    }

    private void updateLeaderboard(BidPlacedEvent event) {
        // Logic to update leaderboard table
    }

    private void updateRefreshStatus(BidPlacedEvent event, String projectionName) {
        RefreshStatus status = refreshStatusRepository.findById(projectionName).orElse(new RefreshStatus());
        status.setProjectionName(projectionName);
        status.setLastEventId(event.getEventId().toString());
        status.setLastProcessedAt(Instant.now());
        refreshStatusRepository.save(status);
    }
}