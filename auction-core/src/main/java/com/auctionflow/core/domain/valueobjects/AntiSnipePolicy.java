package com.auctionflow.core.domain.valueobjects;

import java.time.Duration;

public record AntiSnipePolicy(Duration extensionWindow, int maxExtensions, ExtensionType extensionType, Duration fixedDuration, double percentage) {
    public AntiSnipePolicy {
        if (extensionWindow == null || extensionWindow.isNegative()) {
            throw new IllegalArgumentException("Extension window must be non-negative");
        }
        if (maxExtensions < 0) {
            throw new IllegalArgumentException("Max extensions must be non-negative");
        }
        if (extensionType == null) {
            throw new IllegalArgumentException("Extension type must not be null");
        }
        if (extensionType == ExtensionType.FIXED && (fixedDuration == null || fixedDuration.isNegative())) {
            throw new IllegalArgumentException("Fixed duration must be non-negative for FIXED type");
        }
        if (extensionType == ExtensionType.PERCENTAGE && (percentage < 0 || percentage > 1)) {
            throw new IllegalArgumentException("Percentage must be between 0 and 1 for PERCENTAGE type");
        }
    }

    public static AntiSnipePolicy none() {
        return new AntiSnipePolicy(Duration.ZERO, 0, ExtensionType.NONE, Duration.ZERO, 0.0);
    }

    public static AntiSnipePolicy fixed(Duration extensionWindow, int maxExtensions, Duration fixedDuration) {
        return new AntiSnipePolicy(extensionWindow, maxExtensions, ExtensionType.FIXED, fixedDuration, 0.0);
    }

    public static AntiSnipePolicy percentage(Duration extensionWindow, int maxExtensions, double percentage) {
        return new AntiSnipePolicy(extensionWindow, maxExtensions, ExtensionType.PERCENTAGE, Duration.ZERO, percentage);
    }

    public boolean shouldExtend(long currentExtensions) {
        return extensionType != ExtensionType.NONE && currentExtensions < maxExtensions;
    }

    public Duration calculateExtension(Duration originalDuration) {
        return switch (extensionType) {
            case NONE -> Duration.ZERO;
            case FIXED -> fixedDuration;
            case PERCENTAGE -> Duration.ofMillis((long) (originalDuration.toMillis() * percentage));
        };
    }

    public enum ExtensionType {
        NONE, FIXED, PERCENTAGE
    }
}