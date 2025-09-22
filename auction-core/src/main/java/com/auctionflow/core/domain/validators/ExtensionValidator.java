package com.auctionflow.core.domain.validators;

import com.auctionflow.core.domain.valueobjects.AntiSnipePolicy;

import java.time.Duration;
import java.time.Instant;

public class ExtensionValidator {
    public ValidationResult validate(AntiSnipePolicy policy, int currentExtensions, Instant bidTime, Instant endTime) {
        ValidationResult result = new ValidationResult();

        // Check if bid is within extension window
        Duration timeToEnd = Duration.between(bidTime, endTime);
        if (timeToEnd.isNegative()) {
            result.addError("Bid time is after end time");
        } else if (timeToEnd.compareTo(policy.extensionWindow()) <= 0) {
            // Within window, check if should extend
            if (!policy.shouldExtend(currentExtensions)) {
                result.addError("Maximum extensions reached");
            }
        }

        return result;
    }
}