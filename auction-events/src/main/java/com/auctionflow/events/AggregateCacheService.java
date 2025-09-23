package com.auctionflow.events;

import com.auctionflow.core.domain.aggregates.AggregateRoot;
import com.auctionflow.core.domain.valueobjects.AuctionId;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AggregateCacheService {

    private final Cache<AuctionId, AggregateRoot> aggregateCache = Caffeine.newBuilder()
            .maximumSize(10000) // Adjust based on active auctions
            .expireAfterWrite(10, TimeUnit.MINUTES) // Expire after inactivity
            .build();

    public AggregateRoot get(AuctionId auctionId) {
        return aggregateCache.getIfPresent(auctionId);
    }

    public void put(AuctionId auctionId, AggregateRoot aggregate) {
        aggregateCache.put(auctionId, aggregate);
    }

    public void invalidate(AuctionId auctionId) {
        aggregateCache.invalidate(auctionId);
    }
}