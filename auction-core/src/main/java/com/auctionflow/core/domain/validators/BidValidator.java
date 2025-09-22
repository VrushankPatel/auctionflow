package com.auctionflow.core.domain.validators;

import com.auctionflow.core.domain.valueobjects.BidIncrement;
import com.auctionflow.core.domain.valueobjects.BidderId;
import com.auctionflow.core.domain.valueobjects.Money;

public class BidValidator {
    public ValidationResult validate(Money currentHighestBid, Money reservePrice, BidIncrement bidIncrement, BidderId bidderId, Money bidAmount) {
        ValidationResult result = new ValidationResult();

        // Bidder eligibility
        if (bidderId == null) {
            result.addError("Bidder is not eligible");
        }

        // Reserve price
        if (bidAmount.isLessThan(reservePrice)) {
            result.addError("Bid must meet or exceed the reserve price");
        }

        // Minimum increment
        Money minimumBid = bidIncrement.nextBid(currentHighestBid);
        if (bidAmount.isLessThan(minimumBid)) {
            result.addError("Bid must meet or exceed the minimum increment");
        }

        return result;
    }
}