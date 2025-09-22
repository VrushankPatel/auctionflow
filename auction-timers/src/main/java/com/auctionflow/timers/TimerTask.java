package com.auctionflow.timers;

/**
 * Interface for tasks that can be scheduled in the timing wheel.
 */
public interface TimerTask {

    /**
     * Executes the task.
     */
    void execute();
}