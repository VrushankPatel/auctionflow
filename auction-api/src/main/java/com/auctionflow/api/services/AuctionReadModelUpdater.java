package com.auctionflow.api.services;

import com.auctionflow.api.entities.Auction;
import com.auctionflow.api.repositories.AuctionRepository;
import com.auctionflow.core.domain.events.BidPlacedEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.events.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionReadModelUpdater {

    @Autowired
    private AuctionRepository auctionRepository;

    @EventHandler
    @Transactional
    public void handle(BidPlacedEvent event) {
        Auction auction = auctionRepository.findById(event.getAggregateId().toString()).orElse(null);
        if (auction != null) {
            auction.setCurrentHighestBid(event.getAmount().toBigDecimal());
            auction.setCurrentHighestBidder(Long.parseLong(event.getBidderId()));
            auctionRepository.save(auction);
        }
    }
}