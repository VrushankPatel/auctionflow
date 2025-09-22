package com.auctionflow.core.domain.utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;

/**
 * Simple thread-safe object pool for zero-allocation in hot paths.
 * Uses a blocking queue to manage pooled objects.
 * @param <T> the type of objects to pool
 */
public class ObjectPool<T> {
    private final BlockingQueue<T> pool;
    private final Supplier<T> factory;
    private final int maxSize;

    public ObjectPool(int initialSize, int maxSize, Supplier<T> factory) {
        this.maxSize = maxSize;
        this.factory = factory;
        this.pool = new ArrayBlockingQueue<>(maxSize);
        for (int i = 0; i < initialSize; i++) {
            pool.offer(factory.get());
        }
    }

    /**
     * Borrows an object from the pool, creating a new one if necessary.
     * @return a pooled or new object
     */
    public T borrow() {
        T obj = pool.poll();
        return obj != null ? obj : factory.get();
    }

    /**
     * Returns an object to the pool if there's space.
     * @param obj the object to return
     */
    public void release(T obj) {
        if (obj != null && pool.size() < maxSize) {
            pool.offer(obj);
        }
    }
}