package com.auctionflow.timers;

import com.auctionflow.core.domain.events.AuctionExtendedEvent;
import com.auctionflow.core.domain.events.DomainEvent;
import com.auctionflow.core.domain.valueobjects.AntiSnipePolicy;
import com.auctionflow.core.domain.valueobjects.AuctionId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component

/**
 * Handles anti-snipe extension logic for auctions.
 * Checks if a bid is within the extension window, calculates new end time,
 * reschedules the timer, and emits AuctionExtendedEvent.
 */
public class AntiSnipeExtension {

    private static final Logger logger = LoggerFactory.getLogger(AntiSnipeExtension.class);

    private final HierarchicalTimingWheel timingWheel;
    private final AuctionTimerService timerService; // Assume this exists or will be created

    public AntiSnipeExtension(HierarchicalTimingWheel timingWheel, AuctionTimerService timerService) {
        this.timingWheel = timingWheel;
        this.timerService = timerService;
    }

    /**
     * Checks if extension should apply for a bid and calculates the extension event if necessary.
     *
     * @param auctionId the auction ID
     * @param bidTime the time the bid was placed
     * @param currentEndTime the current end time of the auction
     * @param originalDuration the original duration of the auction
     * @param policy the anti-snipe policy
     * @param currentExtensions the current number of extensions applied
     * @return the AuctionExtendedEvent if extension should be applied, null otherwise
     */
    public AuctionExtendedEvent calculateExtensionIfNeeded(AuctionId auctionId, Instant bidTime, Instant currentEndTime,
                                          Duration originalDuration, AntiSnipePolicy policy, long currentExtensions) {
        // Check if bid is within extension window
        Duration timeToEnd = Duration.between(bidTime, currentEndTime);
        if (timeToEnd.isNegative() || timeToEnd.compareTo(policy.extensionWindow()) > 0) {
            // Bid is not within extension window
            return null;
        }

        // Check if should extend
        if (!policy.shouldExtend(currentExtensions)) {
            logger.info("Max extensions reached for auction {}, not extending", auctionId);
            return null;
        }

        // Calculate extension duration
        Duration extension = policy.calculateExtension(originalDuration);
        if (extension.isZero()) {
            return null;
        }

        // Calculate new end time
        Instant newEndTime = currentEndTime.plus(extension);

        // Reschedule timer
        timerService.rescheduleAuctionClose(auctionId, newEndTime);

        // Create AuctionExtendedEvent
        AuctionExtendedEvent event = new AuctionExtendedEvent(
            auctionId,
            newEndTime,
            UUID.randomUUID(),
            Instant.now(),
            0 // sequence number, might need to be managed properly
        );

        logger.info("Calculated extension for auction {} to new end time {}", auctionId, newEndTime);
        return event;
    }
}