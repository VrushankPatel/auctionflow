package com.auctionflow.common.exceptions;

public class InsufficientBidException extends RuntimeException {
    public InsufficientBidException(String message) {
        super(message);
    }

    public InsufficientBidException(String message, Throwable cause) {
        super(message, cause);
    }
}