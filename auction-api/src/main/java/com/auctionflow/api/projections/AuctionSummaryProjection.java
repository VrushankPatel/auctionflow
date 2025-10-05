package com.auctionflow.api.projections;

import com.auctionflow.api.entities.AuctionDetails;
import com.auctionflow.api.entities.Item;
import com.auctionflow.api.entities.RefreshStatus;
import com.auctionflow.api.repositories.AuctionDetailsRepository;
import com.auctionflow.api.repositories.ItemRepository;
import com.auctionflow.api.repositories.RefreshStatusRepository;
import com.auctionflow.core.domain.events.AuctionClosedEvent;
import com.auctionflow.core.domain.events.AuctionCreatedEvent;
import com.auctionflow.core.domain.events.AuctionExtendedEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.events.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
public class AuctionSummaryProjection {

    private final AuctionDetailsRepository auctionDetailsRepository;
    private final ItemRepository itemRepository;
    private final RefreshStatusRepository refreshStatusRepository;

    public AuctionSummaryProjection(AuctionDetailsRepository auctionDetailsRepository,
                                   ItemRepository itemRepository,
                                   RefreshStatusRepository refreshStatusRepository) {
        this.auctionDetailsRepository = auctionDetailsRepository;
        this.itemRepository = itemRepository;
        this.refreshStatusRepository = refreshStatusRepository;
    }

    @EventHandler
    @Transactional
    public void on(AuctionCreatedEvent event) {
        updateAuctionDetails(event);
        updateRefreshStatus(event, "AuctionSummaryProjection");
    }

    @EventHandler
    @Transactional
    public void on(AuctionExtendedEvent event) {
        updateAuctionDetails(event);
        updateRefreshStatus(event, "AuctionSummaryProjection");
    }

    @EventHandler
    @Transactional
    public void on(AuctionClosedEvent event) {
        updateAuctionDetails(event);
        updateRefreshStatus(event, "AuctionSummaryProjection");
    }

    private void updateAuctionDetails(DomainEvent event) {
        String auctionId = event.getAggregateId().toString();

        if (event instanceof AuctionCreatedEvent) {
            AuctionCreatedEvent createdEvent = (AuctionCreatedEvent) event;
            Optional<Item> itemOpt = itemRepository.findById(createdEvent.getItemId().toString());

            if (itemOpt.isPresent()) {
                Item item = itemOpt.get();
                AuctionDetails details = new AuctionDetails();
                details.setAuctionId(auctionId);
                details.setItemId(item.getId());
                details.setSellerId(item.getSellerId().toString());
                details.setTitle(item.getTitle());
                details.setDescription(item.getDescription());
                details.setCategory(item.getCategoryId());
                details.setAuctionType(createdEvent.getAuctionType().toString());
                details.setStartTs(createdEvent.getStartTime());
                details.setEndTs(createdEvent.getEndTime());
                details.setStatus("ACTIVE");
                details.setReservePrice(createdEvent.getReservePrice() != null ? createdEvent.getReservePrice().toBigDecimal() : null);
                details.setBuyNowPrice(createdEvent.getBuyNowPrice() != null ? createdEvent.getBuyNowPrice().toBigDecimal() : null);
                // TODO: Add increment strategy and extension policy to AuctionCreatedEvent
                details.setIncrementStrategy("FIXED_10_PERCENT");
                details.setExtensionPolicy("ANTI_SNIPE_5_MIN");
                details.setCurrentHighestBid(null);
                details.setHighestBidderId(null);
                details.setBidCount(0);
                details.setCreatedAt(Instant.now());

                auctionDetailsRepository.save(details);
            }
        } else if (event instanceof AuctionExtendedEvent) {
            AuctionExtendedEvent extendedEvent = (AuctionExtendedEvent) event;
            Optional<AuctionDetails> detailsOpt = auctionDetailsRepository.findById(auctionId);
            if (detailsOpt.isPresent()) {
                AuctionDetails details = detailsOpt.get();
                details.setEndTs(extendedEvent.getNewEndTime());
                auctionDetailsRepository.save(details);
            }
        } else if (event instanceof AuctionClosedEvent) {
            Optional<AuctionDetails> detailsOpt = auctionDetailsRepository.findById(auctionId);
            if (detailsOpt.isPresent()) {
                AuctionDetails details = detailsOpt.get();
                details.setStatus("CLOSED");
                auctionDetailsRepository.save(details);
            }
        }
    }

    private void updateRefreshStatus(DomainEvent event, String projectionName) {
        RefreshStatus status = refreshStatusRepository.findById(projectionName).orElse(new RefreshStatus());
        status.setProjectionName(projectionName);
        status.setLastEventId(event.getEventId().toString());
        status.setLastProcessedAt(Instant.now());
        refreshStatusRepository.save(status);
    }
}