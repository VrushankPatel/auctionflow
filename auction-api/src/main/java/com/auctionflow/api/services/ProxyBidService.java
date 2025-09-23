package com.auctionflow.api.services;

import com.auctionflow.api.entities.ProxyBid;
import com.auctionflow.api.repositories.ProxyBidRepository;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.auctionflow.core.domain.valueobjects.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
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
        String auctionIdStr = auctionId.value().toString();

        // Check if user already has a proxy bid for this auction
        Optional<ProxyBid> existing = proxyBidRepository.findByAuctionIdAndUserId(auctionIdStr, userId);
        if (existing.isPresent()) {
            ProxyBid proxyBid = existing.get();
            proxyBid.setMaxBid(maxBid.toBigDecimal());
            proxyBid.setStatus("ACTIVE");
            return proxyBidRepository.save(proxyBid);
        } else {
            ProxyBid proxyBid = new ProxyBid();
            proxyBid.setAuctionId(auctionIdStr);
            proxyBid.setUserId(userId);
            proxyBid.setMaxBid(maxBid.toBigDecimal());
            proxyBid.setCurrentBid(BigDecimal.ZERO);
            proxyBid.setStatus("ACTIVE");
            return proxyBidRepository.save(proxyBid);
        }
    }

    public Optional<ProxyBid> getProxyBid(UUID userId, AuctionId auctionId) {
        return proxyBidRepository.findByAuctionIdAndUserId(auctionId.value().toString(), userId);
    }

    public List<ProxyBid> getActiveProxyBidsForAuction(AuctionId auctionId) {
        return proxyBidRepository.findByAuctionIdAndStatus(auctionId.value().toString(), "ACTIVE");
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
        return proxyBidRepository.findActiveProxyBidsHigherThan(auctionId.value().toString(), currentHighestBid.toBigDecimal());
    }

    /**
     * Calculate the next bid amount for a proxy bid based on increment strategy.
     * For now, simple implementation - can be enhanced with proper increment logic.
     */
    public Money calculateNextBidAmount(Money currentHighest, Money proxyMaxBid) {
        // Simple increment: add 1 to current highest, but not exceeding max bid
        Money nextBid = currentHighest.add(Money.usd(BigDecimal.ONE));
        if (nextBid.isGreaterThan(proxyMaxBid)) {
            return null; // Cannot bid
        }
        return nextBid;
    }
}