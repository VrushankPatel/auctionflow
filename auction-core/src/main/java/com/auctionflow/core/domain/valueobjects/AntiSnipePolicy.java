package com.auctionflow.core.domain.valueobjects;

import java.time.Duration;

public record AntiSnipePolicy(Duration extensionWindow, int maxExtensions, Duration extensionDuration) {
    public AntiSnipePolicy {
        if (extensionWindow == null || extensionWindow.isNegative()) {
            throw new IllegalArgumentException("Extension window must be non-negative");
        }
        if (maxExtensions < 0) {
            throw new IllegalArgumentException("Max extensions must be non-negative");
        }
        if (extensionDuration == null || extensionDuration.isNegative()) {
            throw new IllegalArgumentException("Extension duration must be non-negative");
        }
    }

    public static AntiSnipePolicy none() {
        return new AntiSnipePolicy(Duration.ZERO, 0, Duration.ZERO);
    }

    public boolean shouldExtend(long currentExtensions) {
        return currentExtensions < maxExtensions;
    }
}