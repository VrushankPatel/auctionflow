package com.auctionflow.common.exceptions;

public class BidderNotEligibleException extends RuntimeException {
    public BidderNotEligibleException(String message) {
        super(message);
    }

    public BidderNotEligibleException(String message, Throwable cause) {
        super(message, cause);
    }
}