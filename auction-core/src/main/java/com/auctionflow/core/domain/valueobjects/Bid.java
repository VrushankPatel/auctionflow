package com.auctionflow.core.domain.valueobjects;

import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Pooled Bid object to minimize allocations in high-frequency bid processing.
 * Uses object pooling with ArrayBlockingQueue to reuse instances and reduce GC pressure.
 * Thread-safe for concurrent access in high-throughput scenarios.
 */
public class Bid {
    private static final int POOL_SIZE = 10000;
    private static final BlockingQueue<Bid> POOL = new ArrayBlockingQueue<>(POOL_SIZE);

    static {
        // Pre-populate pool
        for (int i = 0; i < POOL_SIZE; i++) {
            POOL.offer(new Bid());
        }
    }

    private BidderId bidderId;
    private Money amount;
    private Instant timestamp;
    private long seqNo;

    private Bid() {
        // Private constructor for pooling
    }

    /**
     * Borrows a Bid instance from the pool and initializes it.
     * @param bidderId the bidder ID
     * @param amount the bid amount
     * @param timestamp the timestamp
     * @param seqNo the sequence number
     * @return initialized Bid instance
     */
    public static Bid create(BidderId bidderId, Money amount, Instant timestamp, long seqNo) {
        if (bidderId == null) {
            throw new IllegalArgumentException("BidderId cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        Bid bid = POOL.poll();
        if (bid == null) {
            bid = new Bid(); // Fallback if pool exhausted
        }
        bid.bidderId = bidderId;
        bid.amount = amount;
        bid.timestamp = timestamp;
        bid.seqNo = seqNo;
        return bid;
    }

    /**
     * Returns the Bid instance to the pool for reuse.
     * @param bid the bid to release
     */
    public static void release(Bid bid) {
        if (bid != null) {
            bid.reset();
            POOL.offer(bid);
        }
    }

    private void reset() {
        this.bidderId = null;
        this.amount = null;
        this.timestamp = null;
        this.seqNo = 0;
    }

    // Getters
    public BidderId bidderId() { return bidderId; }
    public Money amount() { return amount; }
    public Instant timestamp() { return timestamp; }
    public long seqNo() { return seqNo; }
}