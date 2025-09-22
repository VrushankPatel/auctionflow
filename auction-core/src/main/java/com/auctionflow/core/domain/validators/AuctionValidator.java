package com.auctionflow.core.domain.validators;

import com.auctionflow.core.domain.valueobjects.ItemId;

import java.time.Instant;

public class AuctionValidator {
    public ValidationResult validate(Instant startTime, Instant endTime, ItemId itemId, boolean itemAvailable) {
        ValidationResult result = new ValidationResult();

        // Valid time range
        if (startTime == null || endTime == null) {
            result.addError("Start time and end time must be provided");
        } else {
            if (!startTime.isBefore(endTime)) {
                result.addError("Start time must be before end time");
            }
            if (startTime.isBefore(Instant.now())) {
                result.addError("Start time must be in the future");
            }
        }

        // Item availability
        if (itemId == null) {
            result.addError("Item must be specified");
        }
        if (!itemAvailable) {
            result.addError("Item is not available");
        }

        return result;
    }
}