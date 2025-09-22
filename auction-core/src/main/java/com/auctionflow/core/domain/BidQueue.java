package com.auctionflow.core.domain;

import com.auctionflow.core.domain.valueobjects.Bid;
import com.auctionflow.core.domain.valueobjects.Money;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Efficient bid queue for high-frequency auctions.
 * Uses concurrent priority queue ordered by price-time priority: higher amount first, then lower seqNo for ties.
 * Provides O(log n) insertion and O(1) peek for processing.
 * Thread-safe for concurrent access in high-frequency scenarios.
 */
public class BidQueue {
    private final PriorityBlockingQueue<Bid> queue;

    public BidQueue() {
        this.queue = new PriorityBlockingQueue<>(11, Comparator
                .comparing((Bid b) -> b.amount().getAmountCents(), Comparator.reverseOrder()) // higher amount first
                .thenComparing(Bid::seqNo)); // lower seqNo first for same amount
    }

    /**
     * Adds a bid to the queue.
     * @param bid the bid to add
     */
    public void addBid(Bid bid) {
        queue.offer(bid);
    }

    /**
     * Peeks at the highest priority bid without removing.
     * @return the highest priority bid or null if empty
     */
    public Bid peekHighestBid() {
        return queue.peek();
    }

    /**
     * Removes and returns the highest priority bid.
     * @return the highest priority bid or null if empty
     */
    public Bid pollHighestBid() {
        return queue.poll();
    }

    /**
     * Checks if the queue is empty.
     * @return true if empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns the size of the queue.
     * @return number of bids in queue
     */
    public int size() {
        return queue.size();
    }

    /**
     * Clears all bids from the queue.
     */
    public void clear() {
        queue.clear();
    }
}