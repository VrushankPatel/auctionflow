package com.auctionflow.common.service;

import com.auctionflow.core.domain.valueobjects.AuctionId;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * Redis-based implementation of SequenceService for distributed sequence generation.
 * Uses Redis atomic increment to ensure global monotonicity across all nodes.
 */
@Service
public class RedisSequenceService implements SequenceService {

    private final RedissonClient redissonClient;

    public RedisSequenceService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public long nextSequence(AuctionId auctionId) {
        String key = "auction:seq:" + auctionId.value();
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.incrementAndGet();
    }
}