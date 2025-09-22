package com.auctionflow.payments.events;

import java.time.LocalDateTime;

public class PaymentAuthorizedEvent {
    private final Long paymentId;
    private final String auctionId;
    private final String payerId;
    private final LocalDateTime timestamp;

    public PaymentAuthorizedEvent(Long paymentId, String auctionId, String payerId) {
        this.paymentId = paymentId;
        this.auctionId = auctionId;
        this.payerId = payerId;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public Long getPaymentId() { return paymentId; }
    public String getAuctionId() { return auctionId; }
    public String getPayerId() { return payerId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}