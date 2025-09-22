package com.auctionflow.api.dtos;

import javax.validation.constraints.NotEmpty;
import java.util.List;

public class OfflineSyncRequest {
    @NotEmpty
    private List<PendingBid> pendingBids;

    public OfflineSyncRequest() {}

    public OfflineSyncRequest(List<PendingBid> pendingBids) {
        this.pendingBids = pendingBids;
    }

    public List<PendingBid> getPendingBids() { return pendingBids; }
    public void setPendingBids(List<PendingBid> pendingBids) { this.pendingBids = pendingBids; }

    public static class PendingBid {
        private String auctionId;
        private double amount;
        private String idempotencyKey;

        public PendingBid() {}

        public PendingBid(String auctionId, double amount, String idempotencyKey) {
            this.auctionId = auctionId;
            this.amount = amount;
            this.idempotencyKey = idempotencyKey;
        }

        // Getters and setters
        public String getAuctionId() { return auctionId; }
        public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    }
}