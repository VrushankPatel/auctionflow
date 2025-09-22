package com.auctionflow.api.projections;

import com.auctionflow.api.entities.RefreshStatus;
import com.auctionflow.api.repositories.RefreshStatusRepository;
import com.auctionflow.core.domain.events.AuctionClosedEvent;
import com.auctionflow.core.domain.events.AuctionCreatedEvent;
import com.auctionflow.core.domain.events.AuctionExtendedEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.events.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class AuctionSummaryProjection {

    private final RefreshStatusRepository refreshStatusRepository;

    public AuctionSummaryProjection(RefreshStatusRepository refreshStatusRepository) {
        this.refreshStatusRepository = refreshStatusRepository;
    }

    @EventHandler
    @Transactional
    public void on(AuctionCreatedEvent event) {
        // Immediate update for critical data
        updateAuctionDetails(event);
        updateRefreshStatus(event, "AuctionSummaryProjection");
    }

    @EventHandler
    @Transactional
    public void on(AuctionExtendedEvent event) {
        // Immediate update for critical data
        updateAuctionDetails(event);
        updateRefreshStatus(event, "AuctionSummaryProjection");
    }

    @EventHandler
    @Transactional
    public void on(AuctionClosedEvent event) {
        // Immediate update for critical data
        updateAuctionDetails(event);
        updateRefreshStatus(event, "AuctionSummaryProjection");
    }

    private void updateAuctionDetails(DomainEvent event) {
        // Logic to update auction_details table
        // Assuming we have access to event data
        // For simplicity, using JDBC or JPA to update
        // In real impl, inject JpaRepository or use EntityManager
    }

    private void updateRefreshStatus(DomainEvent event, String projectionName) {
        RefreshStatus status = refreshStatusRepository.findById(projectionName).orElse(new RefreshStatus());
        status.setProjectionName(projectionName);
        status.setLastEventId(event.getEventId().toString());
        status.setLastProcessedAt(Instant.now());
        refreshStatusRepository.save(status);
    }
}