package com.auctionflow.api.services;

import com.auctionflow.api.entities.ProxyBid;
import com.auctionflow.api.repositories.ProxyBidRepository;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProxyBidService {

    private final ProxyBidRepository proxyBidRepository;

    public ProxyBidService(ProxyBidRepository proxyBidRepository) {
        this.proxyBidRepository = proxyBidRepository;
    }

    @Transactional
    public ProxyBid setProxyBid(UUID userId, AuctionId auctionId, Money maxBid) {
        // Check if user already has a proxy bid for this auction
        Optional<ProxyBid> existing = proxyBidRepository.findByAuctionIdAndUserId(auctionId.value(), userId);
        if (existing.isPresent()) {
            ProxyBid proxyBid = existing.get();
            proxyBid.setMaxBid(maxBid.toBigDecimal());
            proxyBid.setStatus("ACTIVE");
            return proxyBidRepository.save(proxyBid);
        } else {
            ProxyBid proxyBid = new ProxyBid();
            proxyBid.setAuctionId(auctionId.value());
            proxyBid.setUserId(userId);
            proxyBid.setMaxBid(maxBid.toBigDecimal());
            proxyBid.setCurrentBid(BigDecimal.ZERO);
            proxyBid.setStatus("ACTIVE");
            return proxyBidRepository.save(proxyBid);
        }
    }

    public Optional<ProxyBid> getProxyBid(UUID userId, AuctionId auctionId) {
        return proxyBidRepository.findByAuctionIdAndUserId(auctionId.value(), userId);
    }

    public List<ProxyBid> getActiveProxyBidsForAuction(AuctionId auctionId) {
        return proxyBidRepository.findByAuctionIdAndStatus(auctionId.value(), "ACTIVE");
    }

    @Transactional
    public void updateProxyBidStatus(Long proxyBidId, String status) {
        proxyBidRepository.updateStatus(proxyBidId, status);
    }

    @Transactional
    public void updateCurrentBid(Long proxyBidId, Money currentBid) {
        proxyBidRepository.updateCurrentBid(proxyBidId, currentBid.toBigDecimal());
    }

    /**
     * Check if any proxy bids should be triggered after a new bid is placed.
     * This should be called after a bid is accepted.
     */
    @Transactional
    public List<ProxyBid> findProxyBidsToTrigger(AuctionId auctionId, Money currentHighestBid) {
        return proxyBidRepository.findActiveProxyBidsHigherThan(auctionId.value(), currentHighestBid.toBigDecimal());
    }

    /**
     * Calculate the next bid amount for a proxy bid based on increment strategy.
     * For now, simple implementation - can be enhanced with proper increment logic.
     */
    public Money calculateNextBidAmount(Money currentHighest, Money proxyMaxBid) {
        // Simple increment: add 1 to current highest, but not exceeding max bid
        Money nextBid = currentHighest.add(new Money(BigDecimal.ONE));
        if (nextBid.isGreaterThan(proxyMaxBid)) {
            return null; // Cannot bid
        }
        return nextBid;
    }
}